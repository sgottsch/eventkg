package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventKGIdMappingLoader {

	private Map<DataSet, Map<String, String>> entityLabelsMap = new HashMap<DataSet, Map<String, String>>();
	private Map<DataSet, Map<String, String>> eventLabelsMap = new HashMap<DataSet, Map<String, String>>();
	private Map<String, String> eventDescriptionsMap = new HashMap<String, String>();

	private int lastEntityId;
	private int lastEventId;

	public boolean loadFromPreviousVersion;

	public EventKGIdMappingLoader(boolean loadFromPreviousVersion) {
		this.loadFromPreviousVersion = loadFromPreviousVersion;
	}

	public void initEntityIdMapping() {

		System.out.println("Init entity ID mapping.");

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets())
			entityLabelsMap.put(dataSet, new HashMap<String, String>());

		FileName fileName = null;
		if (loadFromPreviousVersion)
			fileName = FileName.ALL_TTL_ENTITIES_WITH_TEXTS_PREVIOUS_VERSION;
		else
			fileName = FileName.ALL_TTL_ENTITIES_WITH_TEXTS;

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(fileName);
			while (it.hasNext()) {
				String line = it.nextLine();

				if (line.isEmpty() || line.startsWith("@"))
					continue;

				String[] parts = line.split(" ");

				if (!parts[1].equals("owl:sameAs"))
					continue;

				String entityId = parts[0];
				int entityNo = Integer
						.valueOf(entityId.substring(entityId.lastIndexOf("_") + 1, entityId.length() - 1));
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

				DataSet dataSet = DataSets.getInstance()
						.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));

				entityLabelsMap.get(dataSet).put(entityLabel, entityId);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

		System.out.println(" Finished: Init entity ID mapping");

	}

	public void initEventIdMapping() {

		System.out.println("Init event ID mapping.");

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets()) {
			eventLabelsMap.put(dataSet, new HashMap<String, String>());
		}

		Set<String> eventsWithoutLabels = new HashSet<String>();

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
				int eventNo = Integer.valueOf(entityId.substring(entityId.lastIndexOf("_") + 1, entityId.length() - 1));
				if (eventNo > this.lastEventId)
					this.lastEventId = eventNo;

				eventsWithoutLabels.add(entityId);

				if (!parts[1].equals("owl:sameAs"))
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

				DataSet dataSet = DataSets.getInstance()
						.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));

				eventLabelsMap.get(dataSet).put(entityLabel, entityId);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

		// describe event by its description. But descriptions are not unique
		// (e.g. "Gamescom in Köln"). That's why they are combined with the
		// "extractedFrom". Example:
		// "<https://de.wikipedia.org/wiki/2012>Gamescom in Köln".
		// There could even be mutiple descriptions from the same source.
		// Connect them alphabetically.
		// "ID" is: all "extractedFrom" aplphabetically, then all "description"
		// alphabetically.

		Map<String, Set<String>> descriptions = new HashMap<String, Set<String>>();
		Map<String, Set<String>> otherURLs = new HashMap<String, Set<String>>();

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

				if (!eventsWithoutLabels.contains(entityId))
					continue;

				splitLine = splitLine.substring(splitLine.indexOf(" ") + 1);
				String property = splitLine.substring(0, splitLine.indexOf(" "));

				if (property.equals("dcterms:description") || property.equals("eventKG-s:extractedFrom")) {

					splitLine = StringUtils.stripEnd(splitLine.trim(), "\\.").trim();

					splitLine = splitLine.substring(splitLine.indexOf(" ") + 1);
					String dataSetGraph = splitLine.substring(splitLine.lastIndexOf(" ") + 1);
					splitLine = splitLine.substring(0, splitLine.lastIndexOf(" "));

					DataSet dataSet = DataSets.getInstance()
							.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));

					if (property.equals("dcterms:description")) {
						String description = splitLine;

						description = description.substring(1, description.lastIndexOf("@") - 1).trim();

						// unescape escapped quotations
						description = description.replace("\\\"", "\"");

						if (!descriptions.containsKey(entityId))
							descriptions.put(entityId, new HashSet<String>());

						descriptions.get(entityId).add(dataSet.getId() + ":" + description);
					} else if (property.equals("eventKG-s:extractedFrom")) {
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
			LineIterator.closeQuietly(it);
		}

		Set<String> eventIds = new HashSet<String>();
		eventIds.addAll(descriptions.keySet());
		eventIds.retainAll(otherURLs.keySet());

		int examples = 0;
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
				System.out.println("Map1: " + eventId + " -> " + description);
			}

			examples -= 1;
		}

	}

	public Map<DataSet, Map<String, String>> getEntityLabelsMap() {
		return entityLabelsMap;
	}

	public Map<DataSet, Map<String, String>> getEventLabelsMap() {
		return eventLabelsMap;
	}

	public Map<String, String> getEventDescriptionsMap() {
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

}
