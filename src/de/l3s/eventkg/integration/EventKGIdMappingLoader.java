package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
	private Map<DataSet, Map<String, String>> eventDescriptionsMap = new HashMap<DataSet, Map<String, String>>();

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
				entityLabel = entityLabel.substring(entityLabel.lastIndexOf("/") + 1, entityLabel.length() - 1);
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

	}

	public void initEventIdMapping() {

		System.out.println("Init event ID mapping.");

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets()) {
			eventLabelsMap.put(dataSet, new HashMap<String, String>());
			eventDescriptionsMap.put(dataSet, new HashMap<String, String>());
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
				entityLabel = entityLabel.substring(entityLabel.lastIndexOf("/") + 1, entityLabel.length() - 1);
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

				if (!property.equals("dcterms:description"))
					continue;

				splitLine = StringUtils.stripEnd(splitLine.trim(), "\\.").trim();

				splitLine = splitLine.substring(splitLine.indexOf(" ") + 1);
				String dataSetGraph = splitLine.substring(splitLine.lastIndexOf(" ") + 1);
				splitLine = splitLine.substring(0, splitLine.lastIndexOf(" "));
				String description = splitLine;

				description = description.substring(1, description.lastIndexOf("@") - 1).trim();
				DataSet dataSet = DataSets.getInstance()
						.getDataSetById(dataSetGraph.replace(PrefixEnum.EVENT_KG_GRAPH.getAbbr(), ""));

				eventDescriptionsMap.get(dataSet).put(description, entityId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

	}

	public Map<DataSet, Map<String, String>> getEntityLabelsMap() {
		return entityLabelsMap;
	}

	public Map<DataSet, Map<String, String>> getEventLabelsMap() {
		return eventLabelsMap;
	}

	public Map<DataSet, Map<String, String>> getEventDescriptionsMap() {
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
