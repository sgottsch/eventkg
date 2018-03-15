package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EntityIdGenerator {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private Map<DataSet, Map<String, String>> entityLabelsMap = new HashMap<DataSet, Map<String, String>>();
	private Map<DataSet, Map<String, String>> eventLabelsMap = new HashMap<DataSet, Map<String, String>>();
	private Map<DataSet, Map<String, String>> eventDescriptionsMap = new HashMap<DataSet, Map<String, String>>();
	// private Map<String, String> relationsMap = new HashMap<String, String>();
	// private Map<String, String> linkRelationsMap = new HashMap<String,
	// String>();

	private int lastEntityId;
	private int lastEventId;
	// private int lastRelationId;
	// private int lastLinkRelationId;

	// public void load() {
	//
	// System.out.println("ID Generator: initEventIdMapping");
	// initEventIdMapping();
	// System.out.println(lastEventId);
	//
	// Event event1 = new Event();
	// event1.setWikidataId("Q362");
	// System.out.println(generateEventId(event1, null));
	//
	// System.out.println("");
	// Event event2 = new Event();
	// event2.setWikidataId("Q362");
	// event2.addWikipediaLabel(Language.EN, "World_War_II");
	// event2.addWikipediaLabel(Language.DE, "Zweiter_Weltkrieg");
	// System.out.println(generateEventId(event2, null));
	//
	// System.out.println("");
	// Event event3 = new Event();
	// event3.setWikidataId("Q362");
	// event3.addWikipediaLabel(Language.EN, "World_War_II");
	// event3.addWikipediaLabel(Language.DE, "Erster_Weltkrieg");
	// System.out.println(generateEventId(event3, null));
	//
	// System.out.println("");
	// Event event3b = new Event();
	// event3b.setWikidataId("Q362");
	// event3b.addWikipediaLabel(Language.EN, "World_War_II");
	// event3b.addWikipediaLabel(Language.DE, "Pseudo_Label");
	// System.out.println(generateEventId(event3b, null));
	//
	// System.out.println("");
	// Event event4 = new Event();
	// Map<DataSet, Set<String>> descriptionMap4 = new HashMap<DataSet,
	// Set<String>>();
	// descriptionMap4.put(DataSets.getInstance().getDataSet(Language.EN,
	// Source.WCE), new HashSet<String>());
	// descriptionMap4.get(DataSets.getInstance().getDataSet(Language.EN,
	// Source.WCE)).add(
	// "British Prime Minister Gordon Brown apologises for the post-war
	// treatment of celebrated WWII code-breaker Alan Turing, who was chemically
	// castrated for having homosexual relations.");
	// System.out.println(generateEventId(event4, descriptionMap4));
	//
	// System.out.println("");
	// Event event5 = new Event();
	// Map<DataSet, Set<String>> descriptionMap5 = new HashMap<DataSet,
	// Set<String>>();
	// descriptionMap5.put(DataSets.getInstance().getDataSet(Language.DE,
	// Source.WIKIPEDIA), new HashSet<String>());
	// descriptionMap5.get(DataSets.getInstance().getDataSet(Language.DE,
	// Source.WIKIPEDIA)).add(
	// "Der Alliierte Kontrollrat der Siegermächte des Zweiten Weltkriegs löst
	// durch Gesetz Nr. 46 endgültig den Staat Preußen auf.");
	// System.out.println(generateEventId(event5, descriptionMap5));
	// System.out.println("");
	// descriptionMap5.put(DataSets.getInstance().getDataSet(Language.EN,
	// Source.WIKIPEDIA), new HashSet<String>());
	// descriptionMap5.get(DataSets.getInstance().getDataSet(Language.EN,
	// Source.WIKIPEDIA))
	// .add("The German state of Prussia is officially abolished by the Allied
	// Control Council.");
	// System.out.println(generateEventId(event5, descriptionMap5));
	//
	// System.out.println("ID Generator: initEntityIdMapping");
	// initEntityIdMapping();
	// System.out.println(lastEntityId);
	//
	// System.out.println("ID Generator: initRelationMap");
	// initRelationMap();
	// System.out.println(lastRelationId);
	//
	// System.out.println("ID Generator: initLinkRelationMap");
	// initLinkRelationMap();
	// System.out.println(lastLinkRelationId);
	//
	// }

	public void load() {

		System.out.println("ID Generator: initEventIdMapping");
		initEventIdMapping();
		System.out.println(lastEventId + " / " + this.eventLabelsMap.size());

		System.out.println("ID Generator: initEntityIdMapping");
		initEntityIdMapping();
		System.out.println(lastEntityId + " / " + this.entityLabelsMap.size());

		// System.out.println("ID Generator: initRelationMap");
		// initRelationMap();
		// System.out.println(lastRelationId + " / " +
		// this.relationsMap.size());
		//
		// System.out.println("ID Generator: initLinkRelationMap");
		// initLinkRelationMap();
		// System.out.println(lastLinkRelationId + " / " +
		// this.linkRelationsMap.size());

	}

	public void initEntityIdMapping() {

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets())
			entityLabelsMap.put(dataSet, new HashMap<String, String>());

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_TTL_ENTITIES_WITH_TEXTS_PREVIOUS_VERSION);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

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
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void initEventIdMapping() {

		for (DataSet dataSet : DataSets.getInstance().getAllDataSets()) {
			eventLabelsMap.put(dataSet, new HashMap<String, String>());
			eventDescriptionsMap.put(dataSet, new HashMap<String, String>());
		}

		Set<String> eventsWithoutLabels = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIOUS_VERSION);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

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
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			try {
				br = FileLoader.getReader(FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIOUS_VERSION);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

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
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// private void initRelationMap() {
	// initRelations(FileName.ALL_TTL_EVENTS_OTHER_RELATIONS_PREVIOUS_VERSION);
	// initRelations(FileName.ALL_TTL_ENTITIES_OTHER_RELATIONS_PREVIOUS_VERSION);
	// initRelations(FileName.ALL_TTL_ENTITIES_TEMPORAL_RELATIONS_PREVIOUS_VERSION);
	// }
	//
	// private void initLinkRelationMap() {
	// BufferedReader br = null;
	// try {
	// try {
	// br =
	// FileLoader.getReader(FileName.ALL_TTL_EVENTS_LINK_RELATIOINS_PREVIOUS_VERSION);
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// String previousRelationId = null;
	//
	// GenericRelation relation = new GenericRelation();
	// while ((line = br.readLine()) != null) {
	//
	// if (line.isEmpty() || line.startsWith("@"))
	// continue;
	//
	// if (!line.startsWith("<eventkg_link_relation_"))
	// continue;
	//
	// String[] parts = line.split(" ");
	//
	// String relationId = parts[0];
	//
	// int relationNo = Integer
	// .valueOf(relationId.substring(relationId.lastIndexOf("_") + 1,
	// relationId.length() - 1));
	// if (relationNo > this.lastLinkRelationId)
	// this.lastLinkRelationId = relationNo;
	//
	// if (previousRelationId != null && !relationId.equals(previousRelationId))
	// {
	// this.linkRelationsMap.put(generateLinkRelationId(relation), relationId);
	// relation = new GenericRelation();
	// }
	//
	// previousRelationId = relationId;
	//
	// if (parts[1].equals("rdf:subject")) {
	// Entity dummySubject = new Entity(null);
	// dummySubject.setId(parts[2]);
	// relation.setSubject(dummySubject);
	// } else if (parts[1].equals("rdf:object")) {
	// Entity dummyObject = new Entity(null);
	// dummyObject.setId(parts[2]);
	// relation.setSubject(dummyObject);
	// }
	//
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// br.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// }
	//
	// private void initRelations(FileName fileName) {
	//
	// BufferedReader br = null;
	// try {
	// try {
	// br = FileLoader.getReader(fileName);
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// String previousRelationId = null;
	//
	// GenericRelation relation = new GenericRelation();
	// while ((line = br.readLine()) != null) {
	//
	// if (line.isEmpty() || line.startsWith("@"))
	// continue;
	//
	// if (!line.startsWith("<eventkg_relation_"))
	// continue;
	//
	// String[] parts = line.split(" ");
	//
	// String relationId = parts[0];
	//
	// int relationNo = Integer
	// .valueOf(relationId.substring(relationId.lastIndexOf("_") + 1,
	// relationId.length() - 1));
	// if (relationNo > this.lastRelationId)
	// this.lastRelationId = relationNo;
	//
	// if (previousRelationId != null && !relationId.equals(previousRelationId))
	// {
	// this.relationsMap.put(generateRelationId(relation), relationId);
	// relation = new GenericRelation();
	// }
	//
	// previousRelationId = relationId;
	//
	// if (parts[1].equals("rdf:subject")) {
	// Entity dummySubject = new Entity(null);
	// dummySubject.setId(parts[2]);
	// relation.setSubject(dummySubject);
	// } else if (parts[1].equals("rdf:object")) {
	// Entity dummyObject = new Entity(null);
	// dummyObject.setId(parts[2]);
	// relation.setSubject(dummyObject);
	// } else if (parts[1].equals("sem:roleType")) {
	// relation.setProperty(parts[2]);
	// } else if (parts[1].equals("rdf:type")) {
	// relation.setDataSet(
	// DataSets.getInstance().getDataSetById(parts[3].substring(parts[3].lastIndexOf(":")
	// + 1)));
	// } else if (parts[1].equals("sem:hasBeginTimeStamp")) {
	// String beginTimeStamp = parts[2].substring(1);
	// beginTimeStamp = beginTimeStamp.substring(0,
	// beginTimeStamp.indexOf("\""));
	// try {
	// relation.setStartTime(dateFormat.parse(beginTimeStamp));
	// } catch (ParseException e) {
	// e.printStackTrace();
	// }
	// } else if (parts[1].equals("sem:hasEndTimeStamp")) {
	// String beginTimeStamp = parts[2].substring(1);
	// beginTimeStamp = beginTimeStamp.substring(0,
	// beginTimeStamp.indexOf("\""));
	// try {
	// relation.setStartTime(dateFormat.parse(beginTimeStamp));
	// } catch (ParseException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// br.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }

	public Map<DataSet, Map<String, String>> getEntityLabelsMap() {
		return entityLabelsMap;
	}

	public Map<DataSet, Map<String, String>> getEventLabelsMap() {
		return eventLabelsMap;
	}

	public Map<DataSet, Map<String, String>> getEventDescriptionsMap() {
		return eventDescriptionsMap;
	}

	// public Map<String, String> getRelationsMap() {
	// return relationsMap;
	// }
	//
	// public Map<String, String> getLinkRelationsMap() {
	// return linkRelationsMap;
	// }

	public int getLastEntityNo() {
		return lastEntityId;
	}

	public int getLastEventNo() {
		return lastEventId;
	}

	// public int getLastRelationNo() {
	// return lastRelationId;
	// }
	//
	// public int getLastLinkRelationNo() {
	// return lastLinkRelationId;
	// }

	public static String generateRelationId(GenericRelation relation) {

		List<String> parts = new ArrayList<String>();

		String subjectId = relation.getSubject().getId();
		parts.add(subjectId);

		Entity object = relation.getObject();
		String objectId = "-";
		if (object != null) {
			objectId = relation.getObject().getId();
		}
		parts.add(objectId);

		if (relation.getStartTime() == null)
			parts.add("-");
		else
			parts.add(dateFormat.format(relation.getStartTime()));

		if (relation.getEndTime() == null)
			parts.add("-");
		else
			parts.add(dateFormat.format(relation.getEndTime()));

		parts.add(relation.getDataSet().getId());
		parts.add(relation.getProperty());

		return StringUtils.join(parts, ";@;");
	}

	public static String generateLinkRelationId(GenericRelation relation) {

		List<String> parts = new ArrayList<String>();

		String subjectId = relation.getSubject().getId();
		parts.add(subjectId);

		Entity object = relation.getObject();
		String objectId = "-";
		if (object != null)
			objectId = relation.getObject().getId();
		parts.add(objectId);

		return StringUtils.join(parts, ";@;");
	}

	public static String generateLinkRelationId(String subjectId, String objectId) {

		List<String> parts = new ArrayList<String>();

		parts.add(subjectId);
		parts.add(objectId);

		return StringUtils.join(parts, ";@;");
	}

	// private String generateEventId(Event event, Map<DataSet, Set<String>>
	// descriptionMap) {
	//
	// Set<String> eventIds = new HashSet<String>();
	//
	// if (event.getWikidataId() == null &&
	// event.getWikipediaLabels().isEmpty()) {
	// for (DataSet dataSet : descriptionMap.keySet()) {
	// if (getEventDescriptionsMap().containsKey(dataSet)) {
	// for (String description : descriptionMap.get(dataSet)) {
	// if (getEventDescriptionsMap().get(dataSet).containsKey(description)) {
	// eventIds.add(getEventDescriptionsMap().get(dataSet).get(description));
	// }
	// }
	// }
	// }
	// } else {
	// if (event.getWikidataId() != null && getEventLabelsMap()
	// .containsKey(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA)))
	// {
	// eventIds.add(getEventLabelsMap().get(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA))
	// .get(event.getWikidataId()));
	// }
	//
	// for (Language language : event.getWikipediaLabels().keySet()) {
	// if (event.getWikidataId() != null && getEventLabelsMap()
	// .containsKey(DataSets.getInstance().getDataSet(language,
	// Source.DBPEDIA))) {
	// eventIds.add(getEventLabelsMap().get(DataSets.getInstance().getDataSet(language,
	// Source.DBPEDIA))
	// .get(event.getWikipediaLabels().get(language)));
	// }
	//
	// }
	// }
	//
	// eventIds.remove(null);
	//
	// if (eventIds.size() == 1) {
	// for (String eventId : eventIds) {
	// return eventId;
	// }
	// }
	//
	// return null;
	// }

}
