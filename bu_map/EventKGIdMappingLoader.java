package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class EventKGIdMappingLoader {

	private CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);

	private Map<DataSet, Cache<String, String>> entityLabelsMap;
	private Map<DataSet, Cache<String, String>> eventLabelsMap;
	private Cache<String, String> eventDescriptionsMap;

	private int lastEntityId;
	private int lastEventId;

	public boolean loadFromPreviousVersion;

	public EventKGIdMappingLoader(boolean loadFromPreviousVersion) {
		this.loadFromPreviousVersion = loadFromPreviousVersion;
	}

	public void initEntityIdMapping() {

		System.out.println("Init entity ID mapping.");
		System.out.println(" loadFromPreviousVersion: " + this.loadFromPreviousVersion + ".");

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets())
			entityLabelsMap.put(dataSet,
					cacheManager.createCache("entityLabelsMap" + dataSet.getId(), CacheConfigurationBuilder
							.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(2000).offheap(1, MemoryUnit.GB))
							.build()));

		FileName fileName = null;
		if (loadFromPreviousVersion)
			fileName = FileName.ALL_TTL_ENTITIES_WITH_TEXTS_PREVIOUS_VERSION;
		else
			fileName = FileName.ALL_TTL_ENTITIES_WITH_TEXTS;

		int lineNumber = 0;
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(fileName);
			while (it.hasNext()) {

				if (lineNumber % 500000 == 0)
					System.out.println(" Line " + lineNumber);

				String line = it.nextLine();
				lineNumber += 1;

				if (line.isEmpty() || line.startsWith("@"))
					continue;

				String[] parts = line.split(" ");

				if (!parts[1].equals("owl:sameAs") && !parts[1].equals("<http://www.w3.org/2002/07/owl#sameAs>"))
					continue;

				String entityId = parts[0];
				entityId = entityId.replace("http://eventKG.l3s.uni-hannover.de/resource/", "");
				entityId = entityId.substring(1, entityId.length() - 1);

				int entityNo = Integer.valueOf(entityId.substring(entityId.lastIndexOf("_") + 1));
				if (entityNo > this.lastEntityId)
					this.lastEntityId = entityNo;

				String entityLabel = parts[2];

				if (entityLabel.contains("resource/"))
					entityLabel = entityLabel.substring(entityLabel.lastIndexOf("resource/") + 9,
							entityLabel.length() - 1);
				else
					entityLabel = entityLabel.substring(entityLabel.lastIndexOf("/") + 1, entityLabel.length() - 1);

				// unescape escapped quotations
				entityLabel = entityLabel.replace("\\\"", "\"");

				String dataSetGraph = parts[3];

				DataSet dataSet = null;
				if (DataStoreWriter.ALLOW_DIRECTIVES) {
					dataSet = DataSets.getInstance()
							.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));
				} else {
					String dataSetId = dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getUrlPrefix(), "");
					dataSetId = dataSetId.substring(1, dataSetId.length() - 1);
					dataSet = DataSets.getInstance().getDataSetById(dataSetId);
				}

				entityLabelsMap.get(dataSet).put(entityLabel, entityId);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println(" Finished: Init entity ID mapping");
	}

	public void initEventIdMapping() {

		System.out.println("Init event ID mapping.");
		System.out.println(" loadFromPreviousVersion: " + this.loadFromPreviousVersion + ".");

		this.eventDescriptionsMap = cacheManager.createCache("eventDescriptionsMap", CacheConfigurationBuilder
				.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(2000).offheap(1, MemoryUnit.GB)).build());

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets()) {
			eventLabelsMap
					.put(dataSet,
							cacheManager.createCache("eventLabelsMap" + dataSet.getId(),
									CacheConfigurationBuilder
											.newCacheConfigurationBuilder(String.class, String.class,
													ResourcePoolsBuilder.heap(2000).offheap(1, MemoryUnit.GB))
											.build()));
		}

		Set<String> eventsWithoutLabels = new THashSet<String>();

		FileName fileName = null;
		if (loadFromPreviousVersion)
			fileName = FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIOUS_VERSION;
		else
			fileName = FileName.ALL_TTL_EVENTS_WITH_TEXTS;

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(fileName);
			while (it.hasNext()) {
				String line = it.nextLine();

				if (line.isEmpty() || line.startsWith("@"))
					continue;

				String[] parts = line.split(" ");
				String entityId = parts[0];
				entityId = entityId.replace("http://eventKG.l3s.uni-hannover.de/resource/", "");
				entityId = entityId.substring(1, entityId.length() - 1);

				int eventNo = Integer.valueOf(entityId.substring(entityId.lastIndexOf("_") + 1));
				if (eventNo > this.lastEventId)
					this.lastEventId = eventNo;

				eventsWithoutLabels.add(entityId);

				if (!parts[1].equals("owl:sameAs") && !parts[1].equals("<http://www.w3.org/2002/07/owl#sameAs>"))
					continue;

				eventsWithoutLabels.remove(entityId);

				String entityLabel = parts[2];
				if (entityLabel.contains("resource/"))
					entityLabel = entityLabel.substring(entityLabel.lastIndexOf("resource/") + 9,
							entityLabel.length() - 1);
				else
					entityLabel = entityLabel.substring(entityLabel.lastIndexOf("/") + 1, entityLabel.length() - 1);

				// unescape escapped quotations
				entityLabel = entityLabel.replace("\\\"", "\"");

				String dataSetGraph = parts[3];

				DataSet dataSet = null;
				if (DataStoreWriter.ALLOW_DIRECTIVES) {
					dataSet = DataSets.getInstance()
							.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));
				} else {
					String dataSetId = dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getUrlPrefix(), "");
					dataSetId = dataSetId.substring(1, dataSetId.length() - 1);
					dataSet = DataSets.getInstance().getDataSetById(dataSetId);
				}

				if (!eventLabelsMap.containsKey(dataSet)) {
					System.out.println("Missing data set: " + dataSetGraph);
					continue;
				}

				eventLabelsMap.get(dataSet).put(entityLabel, entityId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// describe event by its description. But descriptions are not unique
		// (e.g. "Gamescom in Köln"). That's why they are combined with the
		// "extractedFrom". Example:
		// "<https://de.wikipedia.org/wiki/2012>Gamescom in Köln".
		// There could even be mutiple descriptions from the same source.
		// Connect them alphabetically.
		// "ID" is: all "extractedFrom" aplphabetically, then all "description"
		// alphabetically.

		Map<String, Set<String>> descriptions = new THashMap<String, Set<String>>();
		Map<String, Set<String>> otherURLs = new THashMap<String, Set<String>>();

		it = null;
		try {
			it = FileLoader.getLineIterator(fileName);
			while (it.hasNext()) {
				String line = it.nextLine();

				// because of quoted strings with space it is not so easy to
				// split the line

				if (line.isEmpty() || line.startsWith("@"))
					continue;

				String splitLine = line;

				String entityId = splitLine.substring(0, splitLine.indexOf(" "));
				entityId = entityId.replace("http://eventKG.l3s.uni-hannover.de/resource/", "");
				entityId = entityId.substring(1, entityId.length() - 1);

				if (!eventsWithoutLabels.contains(entityId))
					continue;

				splitLine = splitLine.substring(splitLine.indexOf(" ") + 1);
				String property = splitLine.substring(0, splitLine.indexOf(" "));

				if (property.equals(
						PrefixEnum.DCTERMS.getPrefixStringWithResource(DataStoreWriter.ALLOW_DIRECTIVES, "description"))
						|| property.equals(PrefixEnum.EVENT_KG_SCHEMA
								.getPrefixStringWithResource(DataStoreWriter.ALLOW_DIRECTIVES, "extractedFrom"))) {

					splitLine = StringUtils.stripEnd(splitLine.trim(), "\\.").trim();

					splitLine = splitLine.substring(splitLine.indexOf(" ") + 1);
					String dataSetGraph = splitLine.substring(splitLine.lastIndexOf(" ") + 1);
					splitLine = splitLine.substring(0, splitLine.lastIndexOf(" "));

					DataSet dataSet = null;
					if (DataStoreWriter.ALLOW_DIRECTIVES) {
						dataSet = DataSets.getInstance()
								.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));
					} else {
						String dataSetId = dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getUrlPrefix(), "");
						dataSetId = dataSetId.substring(1, dataSetId.length() - 1);
						dataSet = DataSets.getInstance().getDataSetById(dataSetId);
					}

					if (property.equals(PrefixEnum.DCTERMS.getPrefixStringWithResource(DataStoreWriter.ALLOW_DIRECTIVES,
							"description"))) {
						String description = splitLine;

						description = description.substring(1, description.lastIndexOf("@") - 1).trim();

						// unescape escapped quotations
						description = description.replace("\\\"", "\"");

						if (!descriptions.containsKey(entityId))
							descriptions.put(entityId, new HashSet<String>());

						descriptions.get(entityId).add(dataSet.getId() + ":" + description);
					} else if (property.equals(PrefixEnum.EVENT_KG_SCHEMA
							.getPrefixStringWithResource(DataStoreWriter.ALLOW_DIRECTIVES, "extractedFrom"))) {
						String url = splitLine;

						url = url.substring(1, url.lastIndexOf(">")).trim();
						// unescape escapped quotations
						url = url.replace("\\\"", "\"");

						if (!otherURLs.containsKey(entityId))
							otherURLs.put(entityId, new HashSet<String>());

						otherURLs.get(entityId).add(dataSet.getId() + ":" + url);
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Descriptions: " + descriptions.size());
		System.out.println("otherURLs: " + otherURLs.size());

		Set<String> eventIds = new HashSet<String>();
		eventIds.addAll(descriptions.keySet());
		eventIds.retainAll(otherURLs.keySet());

		System.out.println("Description Map? eventIds: " + eventIds.size());

		int examples = 10;
		for (String eventId : eventIds) {

			List<String> otherUrlsOfEvent = new ArrayList<String>();
			otherUrlsOfEvent.addAll(otherURLs.get(eventId));
			Collections.sort(otherUrlsOfEvent);

			List<String> descriptionsOfEvent = new ArrayList<String>();
			descriptionsOfEvent.addAll(descriptions.get(eventId));
			Collections.sort(descriptionsOfEvent);

			String description = StringUtils.join(otherUrlsOfEvent, ";") + "-"
					+ StringUtils.join(descriptionsOfEvent, ";");

			eventDescriptionsMap.put(description, eventId);

			if (examples > 0) {
				System.out.println("Description Map1: " + eventId + " -> " + description);
			}

			examples -= 1;
		}

	}

	public Map<DataSet, Cache<String, String>> getEntityLabelsMap() {
		return entityLabelsMap;
	}

	public Map<DataSet, Cache<String, String>> getEventLabelsMap() {
		return eventLabelsMap;
	}

	public Cache<String, String> getEventDescriptionsMap() {
		return eventDescriptionsMap;
	}

	public int getLastEntityId() {
		return lastEntityId;
	}

	public int getLastEventId() {
		return lastEventId;
	}

	public boolean isLoadFromPreviousVersion() {
		return loadFromPreviousVersion;
	}

	public String getEventKGId(DataSet dataSet, String identifier) {

		if (eventLabelsMap.containsKey(dataSet) && eventLabelsMap.get(dataSet).containsKey(identifier))
			return eventLabelsMap.get(dataSet).get(identifier);

		if (!entityLabelsMap.containsKey(dataSet))
			return null;

		return entityLabelsMap.get(dataSet).get(identifier);
	}

	public void close() {
		this.cacheManager.close();
	}

}
