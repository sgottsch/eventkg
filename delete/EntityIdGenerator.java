package de.l3s.eventkg.integration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.GenericRelation;

public class EntityIdGenerator {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	// private Map<String, String> relationsMap = new HashMap<String, String>();
	// private Map<String, String> linkRelationsMap = new HashMap<String,
	// String>();

	// private int lastRelationId;
	// private int lastLinkRelationId;
	private EventKGIdMappingLoader mappingLoader;

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

		this.mappingLoader = new EventKGIdMappingLoader(false);

		System.out.println("ID Generator: initEventIdMapping");
		mappingLoader.initEventIdMapping();
		System.out.println("Last event ID: " + this.mappingLoader.getLastEventId() + " / event labels: "
				+ this.mappingLoader.getEventLabelsMap().size());

		System.out.println("ID Generator: initEntityIdMapping");
		mappingLoader.initEntityIdMapping();
		System.out.println("Last entity ID: " + this.mappingLoader.getLastEntityId() + " / entity labels: "
				+ this.mappingLoader.getEntityLabelsMap().size());

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

	public Map<DataSet, Cache<String, String>> getEntityLabelsMap() {
		return this.mappingLoader.getEntityLabelsMap();
	}

	public Map<DataSet, Cache<String, String>> getEventLabelsMap() {
		return this.mappingLoader.getEventLabelsMap();
	}

	public Cache<String, String> getEventDescriptionsMap() {
		return this.mappingLoader.getEventDescriptionsMap();
	}

	// public Map<String, String> getRelationsMap() {
	// return relationsMap;
	// }
	//
	// public Map<String, String> getLinkRelationsMap() {
	// return linkRelationsMap;
	// }

	public int getLastEntityNo() {
		return this.mappingLoader.getLastEntityId();
	}

	public int getLastEventNo() {
		return this.mappingLoader.getLastEventId();
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

	public void close() {
		this.mappingLoader.close();
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
