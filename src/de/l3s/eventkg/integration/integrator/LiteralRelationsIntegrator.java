package de.l3s.eventkg.integration.integrator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.SubProperty;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.Pipeline;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LiteralRelationsIntegrator extends Extractor {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private Map<String, PropertyGroup> propertyGroups = new HashMap<String, PropertyGroup>();

	public static void main(String[] args) {

		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		Pipeline.initDataSets(languages);

		try {
			createDummyDataset();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		LiteralRelationsIntegrator ri = new LiteralRelationsIntegrator(languages);
		ri.loadPropertyGroups();

		ri.integrateRelations();
	}

	private static void createDummyDataset() throws ParseException {

		Entity e1 = new Entity();
		e1.setId("e1");
		DataStore.getInstance().getEntities().add(e1);
		Entity e2 = new Entity();
		e2.setId("e2");
		DataStore.getInstance().getEntities().add(e2);
		Entity e3 = new Entity();
		e3.setId("e3");
		DataStore.getInstance().getEntities().add(e3);
		Entity e4 = new Entity();
		e4.setId("e4");
		DataStore.getInstance().getEntities().add(e4);

		String l1 = "l1";
		String l2 = "l2";
		String l3 = "l3";

		// e1 marriedTo e2
		LiteralRelation r1 = new LiteralRelation();
		r1.setSubject(e1);
		r1.setObject(l1);
		r1.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r1.setProperty("yago:isMarriedTo");
		DataStore.getInstance().getLiteralRelations().add(r1);

		LiteralRelation r2 = new LiteralRelation();
		r2.setSubject(e1);
		r2.setObject(l1);
		r2.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-10"), DateGranularity.DAY));
		r2.setProperty("wikidata:P26");
		DataStore.getInstance().getLiteralRelations().add(r2);

		LiteralRelation r3 = new LiteralRelation();
		r3.setSubject(e1);
		r3.setObject(l1);
		r3.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r3.setProperty("dbpedia_en:spouse");
		DataStore.getInstance().getLiteralRelations().add(r3);

		LiteralRelation r3b = new LiteralRelation();
		r3b.setSubject(e1);
		r3b.setObject(l2);
		r3b.setProperty("dbpedia_en:spouse");
		DataStore.getInstance().getLiteralRelations().add(r3b);

		// single relation

		LiteralRelation r4 = new LiteralRelation();
		r4.setSubject(e1);
		r4.setObject(l3);
		r4.setProperty("yago:isMarriedTo");
		DataStore.getInstance().getLiteralRelations().add(r4);

		// e2 died in e3

		LiteralRelation r5 = new LiteralRelation();
		r5.setSubject(e2);
		r5.setObject(l3);
		r5.setProperty("dbpedia_en:deathPlace");
		DataStore.getInstance().getLiteralRelations().add(r5);

		LiteralRelation r6 = new LiteralRelation();
		r6.setSubject(e2);
		r6.setObject(l3);
		r6.setProperty("wikidata:P20");
		DataStore.getInstance().getLiteralRelations().add(r6);

		// single relation

		LiteralRelation r7 = new LiteralRelation();
		r7.setSubject(e2);
		r7.setObject(l3);
		r7.setProperty("dbpedia_en:publisher");
		DataStore.getInstance().getLiteralRelations().add(r7);

	}

	public LiteralRelationsIntegrator(List<Language> languages) {
		super("RelationsIntegrator", Source.ALL, "Fuses relations from different sources.", languages);
	}

	public void run() {
		// System.out.println("Integrate times by earliest and latest time.");
		// integrateTimesByTime();
		System.out.println("Integrate relations that share subject, object and time, and have equivalent properties.");
		loadPropertyGroups();
		integrateRelations();
	}

	private void loadPropertyGroups() {

		System.out.println("loadPropertyGroups");

		// 1. FileName.YAGO_FROM_DBPEDIA_RELATIONS
		// 2. FileName.YAGO_TO_DBPEDIA_RELATIONS
		// 3. FileName.WIKIDATA_PROPERTY_EQUALITIES

		Map<String, Set<PropertyGroup>> propertyGroupsByProperty = new HashMap<String, Set<PropertyGroup>>();
		Map<PropertyGroup, Set<String>> propertyByPropertyGroup = new HashMap<PropertyGroup, Set<String>>();
		Set<PropertyGroup> propertyGroups = new HashSet<PropertyGroup>();

		for (String line : FileLoader.readLines(FileName.YAGO_FROM_DBPEDIA_RELATIONS)) {

			// manually remove erroneous mapping
			if (line.contains("dbp:ontology/goldenRaspberryAward"))
				continue;

			String[] parts = line.split("\t");
			double confidence = Double.valueOf(parts[2]);
			if (confidence >= 0.51) {
				String yagoProperty = parts[1];

				if (!yagoProperty.startsWith("y:"))
					continue;
				if (yagoProperty.endsWith("-"))
					continue;

				yagoProperty = DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO).getId() + ":"
						+ yagoProperty.substring(2);

				String otherProperty = parts[0];
				if (!otherProperty.startsWith("dbp:ontology/"))
					continue;
				if (otherProperty.endsWith("-"))
					continue;

				String dbpediaProperty = DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA).getId() + ":"
						+ otherProperty.substring(13);

				Set<String> properties = new HashSet<String>();
				properties.add(yagoProperty);
				properties.add(dbpediaProperty);
				PropertyGroup propertyGroup = new PropertyGroup(properties);
				propertyGroups.add(propertyGroup);
			}
		}

		for (String line : FileLoader.readLines(FileName.YAGO_TO_DBPEDIA_RELATIONS)) {

			// manually remove erroneous mapping
			if (line.contains("dbp:ontology/goldenRaspberryAward"))
				continue;

			String[] parts = line.split("\t");
			double confidence = Double.valueOf(parts[2]);
			if (confidence >= 0.51) {
				String yagoProperty = parts[0];

				if (!yagoProperty.startsWith("y:"))
					continue;
				if (yagoProperty.endsWith("-"))
					continue;

				yagoProperty = DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO).getId() + ":"
						+ yagoProperty.substring(2);

				String otherProperty = parts[1];
				if (!otherProperty.startsWith("dbp:ontology/"))
					continue;
				if (otherProperty.endsWith("-"))
					continue;

				String dbpediaProperty = DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA).getId() + ":"
						+ otherProperty.substring(13);
				Set<String> properties = new HashSet<String>();
				properties.add(yagoProperty);
				properties.add(dbpediaProperty);
				PropertyGroup propertyGroup = new PropertyGroup(properties);
				propertyGroups.add(propertyGroup);
			}
		}

		try {
			JSONObject wikidataJSON = new JSONObject(FileLoader.readFile(FileName.WIKIDATA_PROPERTY_EQUALITIES));
			JSONArray bindings = wikidataJSON.getJSONObject("results").getJSONArray("bindings");
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject binding = bindings.getJSONObject(i);

				JSONObject property2JSON = binding.getJSONObject("property2");
				String property2 = property2JSON.getString("value");
				if (property2.startsWith("http://dbpedia.org/ontology/")) {
					property2 = DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA).getId() + ":"
							+ property2.substring(property2.lastIndexOf("/") + 1);
				} else
					continue;

				JSONObject property1JSON = binding.getJSONObject("property1");
				String property1 = property1JSON.getString("value");
				property1 = DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA).getId() + ":"
						+ property1.substring(property1.lastIndexOf("/") + 1);

				Set<String> properties = new HashSet<String>();
				properties.add(property1);
				properties.add(property2);
				PropertyGroup propertyGroup = new PropertyGroup(properties);
				propertyGroups.add(propertyGroup);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// merge property groups

		for (PropertyGroup propertyGroup : propertyGroups) {
			propertyByPropertyGroup.put(propertyGroup, new HashSet<String>());
			for (String property : propertyGroup.getProperties()) {
				if (!propertyGroupsByProperty.containsKey(property))
					propertyGroupsByProperty.put(property, new HashSet<PropertyGroup>());
				propertyGroupsByProperty.get(property).add(propertyGroup);
				propertyByPropertyGroup.get(propertyGroup).add(property);
			}
		}

		boolean changed = true;
		while (changed) {

			changed = false;

			for (String property : propertyGroupsByProperty.keySet()) {

				// find a property with multiple property groups
				if (propertyGroupsByProperty.get(property).size() > 1) {
					changed = true;

					Set<String> mergedProperties = new HashSet<String>();

					// merge together all the property groups of the property
					Set<PropertyGroup> groupsToRemove = new HashSet<PropertyGroup>();
					for (PropertyGroup group : propertyGroupsByProperty.get(property)) {
						mergedProperties.addAll(group.getProperties());
						// remove the old property groups
						propertyGroups.remove(group);
						groupsToRemove.add(group);
					}

					// create new property groups
					PropertyGroup mergedGroup = new PropertyGroup(mergedProperties);
					propertyGroups.add(mergedGroup);

					// remove old property groups from property -> group mapping
					Set<String> properties2 = new HashSet<String>();
					for (PropertyGroup group : groupsToRemove) {
						for (String property2 : mergedProperties) {
							propertyGroupsByProperty.get(property2).remove(group);
							propertyGroupsByProperty.get(property2).add(mergedGroup);
						}
					}

					propertyGroups.add(mergedGroup);
					for (PropertyGroup groupToRemove : groupsToRemove)
						propertyByPropertyGroup.remove(groupToRemove);

					propertyByPropertyGroup.put(mergedGroup, new HashSet<String>());
					for (String property2 : properties2)
						propertyByPropertyGroup.get(mergedGroup).add(property2);

				}
			}

		}

		this.propertyGroups = new HashMap<String, PropertyGroup>();
		for (PropertyGroup propertyGroup : propertyGroups) {
			for (String property : propertyGroup.getProperties()) {
				this.propertyGroups.put(property, propertyGroup);
			}
		}

		System.out.println("#Property Groups: " + propertyGroups.size());

		// System.out.println("PropertyGroups");
		// for (PropertyGroup propertyGroup : propertyGroups) {
		// for (String property : propertyGroup.getProperties()) {
		// System.out.println(property);
		// }
		// System.out.println("");
		// }

	}

	private class PropertyGroup {

		private Set<String> properties;

		public PropertyGroup(Set<String> properties) {
			super();
			this.properties = properties;
		}

		public Set<String> getProperties() {
			return properties;
		}

	}

	private void integrateRelations() {
		Set<Set<LiteralRelation>> relationGroupCandidates = buildRelationGroupCandidates();
		System.out.println("#literalRelationGroupCandidates: " + relationGroupCandidates.size());
		Set<Set<LiteralRelation>> relationGroups = buildRelationGroups(relationGroupCandidates);
		System.out.println("#literal relation groups 3: " + relationGroups.size());
		integrateRelationGroups(relationGroups);
	}

	private Set<Set<LiteralRelation>> buildRelationGroups(Set<Set<LiteralRelation>> relationGroupCandidates) {
		Set<Set<LiteralRelation>> relationGroups1 = groupRelationGroupsByPropertyGroups(relationGroupCandidates);
		System.out.println("#literal relation groups 1: " + relationGroups1.size());
		Set<Set<LiteralRelation>> relationGroups = groupRelationGroupsByTime(relationGroups1);
		System.out.println("#literal relation groups 2: " + relationGroups.size());
		return relationGroups;
	}

	private Set<Set<LiteralRelation>> groupRelationGroupsByTime(Set<Set<LiteralRelation>> relationGroupCandidates) {
		Set<Set<LiteralRelation>> relationGroups = new HashSet<Set<LiteralRelation>>();

		for (Set<LiteralRelation> relationGroup : relationGroupCandidates) {

			if (relationGroup.size() <= 1)
				continue;

			// System.out.println("GROUP");
			// for (LiteralRelation rel : relationGroup)
			// System.out.println(printDummyRelation(rel));
			// System.out.println("");

			Map<String, Set<LiteralRelation>> relationsByTimeGroup = new HashMap<String, Set<LiteralRelation>>();

			for (LiteralRelation relation : relationGroup) {
				String timeGroup = "";
				if (relation.getStartTime() != null)
					timeGroup += dateFormat.format(relation.getStartTime().getDate());
				timeGroup += "-";
				if (relation.getEndTime() != null)
					timeGroup += dateFormat.format(relation.getEndTime().getDate());
				if (!relationsByTimeGroup.containsKey(timeGroup))
					relationsByTimeGroup.put(timeGroup, new HashSet<LiteralRelation>());
				relationsByTimeGroup.get(timeGroup).add(relation);
			}

			for (String timeGroup : relationsByTimeGroup.keySet()) {
				if (relationsByTimeGroup.get(timeGroup).size() > 1) {
					relationGroups.add(relationsByTimeGroup.get(timeGroup));
					// add all relations without start or end time
					if (relationsByTimeGroup.containsKey("-"))
						relationsByTimeGroup.get(timeGroup).addAll(relationsByTimeGroup.get("-"));
				}
			}

		}

		// for (Set<LiteralRelation> relationGroup : relationGroups) {
		// System.out.println("GROUP X");
		// for (LiteralRelation rel : relationGroup)
		// System.out.println(printDummyRelation(rel));
		// System.out.println("");
		// }

		return relationGroups;
	}

	private Set<Set<LiteralRelation>> groupRelationGroupsByPropertyGroups(
			Set<Set<LiteralRelation>> relationGroupCandidates) {

		Set<Set<LiteralRelation>> relationGroups = new HashSet<Set<LiteralRelation>>();

		for (Set<LiteralRelation> relationGroup : relationGroupCandidates) {

			// group candidate relation groups (relations with same subject and
			// object) by property groups
			Map<PropertyGroup, Set<LiteralRelation>> relationsByPropertyGroup = new HashMap<PropertyGroup, Set<LiteralRelation>>();

			for (LiteralRelation relation : relationGroup) {
				PropertyGroup propertyGroup = propertyGroups.get(buildPropertyString(relation));
				if (propertyGroup != null) {
					if (!relationsByPropertyGroup.containsKey(propertyGroup))
						relationsByPropertyGroup.put(propertyGroup, new HashSet<LiteralRelation>());
					relationsByPropertyGroup.get(propertyGroup).add(relation);
				}
			}

			for (PropertyGroup propertyGroup : relationsByPropertyGroup.keySet()) {
				if (relationsByPropertyGroup.get(propertyGroup).size() > 1)
					relationGroups.add(relationsByPropertyGroup.get(propertyGroup));
			}

		}

		return relationGroups;
	}

	private String buildPropertyString(LiteralRelation relation) {
		return relation.getDataSet().getId() + ":" + relation.getProperty();
	}

	private Set<Set<LiteralRelation>> buildRelationGroupCandidates() {

		System.out.println("buildRelationGroupCandidates");

		Map<Entity, Map<String, Set<LiteralRelation>>> relationMap = new HashMap<Entity, Map<String, Set<LiteralRelation>>>();

		Set<Set<LiteralRelation>> relationGroupCandidates = new HashSet<Set<LiteralRelation>>();

		for (LiteralRelation relation : DataStore.getInstance().getLiteralRelations()) {
			if (!relationMap.containsKey(relation.getSubject()))
				relationMap.put(relation.getSubject(), new HashMap<String, Set<LiteralRelation>>());
			if (!relationMap.get(relation.getSubject()).containsKey(relation.getObject()))
				relationMap.get(relation.getSubject()).put(relation.getObject(), new HashSet<LiteralRelation>());
			relationMap.get(relation.getSubject()).get(relation.getObject()).add(relation);
		}

		for (Entity subject : relationMap.keySet()) {
			for (String object : relationMap.get(subject).keySet()) {
				if (relationMap.get(subject).get(object).size() > 1)
					relationGroupCandidates.add(relationMap.get(subject).get(object));
			}
		}

		return relationGroupCandidates;
	}

	// private String printDummyRelation(LiteralRelation relation) {
	// String startTime = "X";
	// if (relation.getStartTime() != null)
	// startTime = dateFormat.format(relation.getStartTime().getDate());
	// String endTime = "X";
	// if (relation.getEndTime() != null)
	// endTime = dateFormat.format(relation.getEndTime().getDate());
	//
	// return relation.getSubject().getWikidataId() + "-" +
	// relation.getProperty() + "-" + relation.getObject() + "-"
	// + startTime + "-" + endTime;
	// }

	private void integrateRelationGroups(Set<Set<LiteralRelation>> relationGroups) {

		// for (Set<LiteralRelation> relationGroup : relationGroups) {
		// System.out.println("GROUP X");
		// for (LiteralRelation rel : relationGroup)
		// System.out.println(printDummyRelation(rel));
		// System.out.println("");
		// }

		System.out.println("Groups of literal relations that can be merged: " + relationGroups.size());

		int numberOfExamples = 3;

		if (numberOfExamples > 0)
			System.out.println("Literal Relation Group:");

		for (LiteralRelation lr : DataStore.getInstance().getLiteralRelations()) {
			if (lr.getProperties() == null && lr.getPrefix() == null) {
				System.out.println("1: Relation without properties and without prefix: "
						+ lr.getSubject().getWikidataId() + "\t" + lr.getObject());
			}
		}

		for (Set<LiteralRelation> relationGroup : relationGroups) {

			LiteralRelation newRelation = new LiteralRelation();

			// all relations have the same subject and object
			for (LiteralRelation relation : relationGroup) {

				if (numberOfExamples > 0) {
					System.out.println(relation.getSubject().getWikidataId() + "\t" + relation.getProperty() + "\t"
							+ relation.getObject());
				}

				if (newRelation.getSubject() == null) {
					newRelation.setSubject(relation.getSubject());
					newRelation.setObject(relation.getObject());
				}

				if (relation.getStartTime() != null && newRelation.getStartTime() == null)
					newRelation.setStartTime(relation.getStartTime());
				if (relation.getEndTime() != null && newRelation.getEndTime() == null)
					newRelation.setEndTime(relation.getEndTime());
			}

			newRelation.setProperties(new HashSet<SubProperty>());
			for (LiteralRelation relation : relationGroup) {
				newRelation.getProperties()
						.add(new SubProperty(relation.getProperty(), relation.getDataSet(), relation.getPrefix()));
			}

			// remove old relations
			DataStore.getInstance().getLiteralRelations().removeAll(relationGroup);
			// add new merged relation
			DataStore.getInstance().getLiteralRelations().add(newRelation);

			if (newRelation.getProperties() == null && newRelation.getPrefix() == null) {
				System.out.println("Prefix and properties of new relation are null.");
			}

			if (numberOfExamples > 0)
				System.out.println("");

			numberOfExamples -= 1;
		}

		for (LiteralRelation lr : DataStore.getInstance().getLiteralRelations()) {
			if (lr.getProperties() == null && lr.getPrefix() == null) {
				System.out.println("2: Relation without properties and without prefix: "
						+ lr.getSubject().getWikidataId() + "\t" + lr.getObject());
			}
		}

	}

}
