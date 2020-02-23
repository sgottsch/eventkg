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
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.SubProperty;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.Pipeline;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RelationsIntegrator extends Extractor {

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

		RelationsIntegrator ri = new RelationsIntegrator(languages);
		ri.loadPropertyGroups();

		ri.integrateRelations();
	}

	private static void createDummyDataset() throws ParseException {

		Entity e1 = new Entity();
		e1.setId("e1");
		e1.setWikidataId("e1");
	//	DataStore.getInstance().getEntities().add(e1);
		Entity e2 = new Entity();
		e2.setId("e2");
		e2.setWikidataId("e2");
	//	DataStore.getInstance().getEntities().add(e2);
		Entity e3 = new Entity();
		e3.setId("e3");
		e3.setWikidataId("e3");
	//	DataStore.getInstance().getEntities().add(e3);
		Entity e4 = new Entity();
		e4.setId("e4");
		e4.setWikidataId("e4");
	//	DataStore.getInstance().getEntities().add(e4);

		// e1 marriedTo e2
		GenericRelation r1 = new GenericRelation();
		r1.setSubject(e1);
		r1.setObject(e2);
		r1.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r1.setProperty("isMarriedTo");
		r1.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getGenericRelations().add(r1);

		GenericRelation r2 = new GenericRelation();
		r2.setSubject(e1);
		r2.setObject(e2);
		r2.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-10"), DateGranularity.DAY));
		r2.setProperty("P26");
		r2.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getGenericRelations().add(r2);

		GenericRelation r3 = new GenericRelation();
		r3.setSubject(e1);
		r3.setObject(e2);
		r3.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r3.setProperty("spouse");
		r3.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r3);

		GenericRelation r3b = new GenericRelation();
		r3b.setSubject(e1);
		r3b.setObject(e2);
		r3b.setProperty("spouse");
		r3b.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r3b);

		// single relation

		GenericRelation r4 = new GenericRelation();
		r4.setSubject(e1);
		r4.setObject(e3);
		r4.setProperty("isMarriedTo");
		r4.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getGenericRelations().add(r4);

		// e2 died in e3

		GenericRelation r5 = new GenericRelation();
		r5.setSubject(e2);
		r5.setObject(e3);
		r5.setProperty("deathPlace");
		r5.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r5);

		GenericRelation r6 = new GenericRelation();
		r6.setSubject(e2);
		r6.setObject(e3);
		r6.setProperty("P20"); // place of death
		r6.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getGenericRelations().add(r6);

		// single relation

		GenericRelation r7 = new GenericRelation();
		r7.setSubject(e2);
		r7.setObject(e3);
		r7.setProperty("publisher");
		r7.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r7);

		// two identical relations

		GenericRelation r8 = new GenericRelation();
		r8.setSubject(e2);
		r8.setObject(e3);
		r8.setProperty("abcde");
		r8.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r8);

		GenericRelation r9 = new GenericRelation();
		r9.setSubject(e2);
		r9.setObject(e3);
		r9.setProperty("abcde");
		r9.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r9);

	}

	public RelationsIntegrator(List<Language> languages) {
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

		// add all DBpedia language editions
		for (PropertyGroup propertyGroup : propertyGroups) {
			Set<String> newProperties = new HashSet<String>();
			for (String property : propertyGroup.getProperties()) {
				if (property.startsWith("dbpedia_en:")) {
					for (Language language : languages) {
						if (language == Language.EN)
							continue;
						String newProperty = property.replace("dbpedia_en",
								"dbpedia_" + language.getLanguageLowerCase());
						newProperties.add(newProperty);
						this.propertyGroups.put(newProperty, propertyGroup);
					}
				}
			}
			propertyGroup.getProperties().addAll(newProperties);
		}

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
		Set<Set<GenericRelation>> relationGroupCandidates = buildRelationGroupCandidates();
		System.out.println("#relationGroupCandidates: " + relationGroupCandidates.size());
		Set<Set<GenericRelation>> relationGroups = buildRelationGroups(relationGroupCandidates);
		integrateRelationGroups(relationGroups);
	}

	private Set<Set<GenericRelation>> buildRelationGroups(Set<Set<GenericRelation>> relationGroupCandidates) {
		Set<Set<GenericRelation>> relationGroups1 = groupRelationGroupsByPropertyGroups(relationGroupCandidates);
		Set<Set<GenericRelation>> relationGroups = groupRelationGroupsByTime(relationGroups1);
		return relationGroups;
	}

	private Set<Set<GenericRelation>> groupRelationGroupsByTime(Set<Set<GenericRelation>> relationGroupCandidates) {
		Set<Set<GenericRelation>> relationGroups = new HashSet<Set<GenericRelation>>();

		for (Set<GenericRelation> relationGroup : relationGroupCandidates) {

			if (relationGroup.size() <= 1)
				continue;

			// System.out.println("GROUP");
			// for (GenericRelation rel : relationGroup)
			// System.out.println(printDummyRelation(rel));
			// System.out.println("");

			Map<String, Set<GenericRelation>> relationsByTimeGroup = new HashMap<String, Set<GenericRelation>>();

			for (GenericRelation relation : relationGroup) {
				String timeGroup = "";
				if (relation.getStartTime() != null)
					timeGroup += dateFormat.format(relation.getStartTime().getDate());
				timeGroup += "-";
				if (relation.getEndTime() != null)
					timeGroup += dateFormat.format(relation.getEndTime().getDate());
				if (!relationsByTimeGroup.containsKey(timeGroup))
					relationsByTimeGroup.put(timeGroup, new HashSet<GenericRelation>());
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

		// for (Set<GenericRelation> relationGroup : relationGroups) {
		// System.out.println("GROUP X");
		// for (GenericRelation rel : relationGroup)
		// System.out.println(printDummyRelation(rel));
		// System.out.println("");
		// }

		return relationGroups;
	}

	private Set<Set<GenericRelation>> groupRelationGroupsByPropertyGroups(
			Set<Set<GenericRelation>> relationGroupCandidates) {

		Set<Set<GenericRelation>> relationGroups = new HashSet<Set<GenericRelation>>();

		for (Set<GenericRelation> relationGroup : relationGroupCandidates) {

			// group candidate relation groups (relations with same subject and
			// object) by property groups
			Map<PropertyGroup, Set<GenericRelation>> relationsByPropertyGroup = new HashMap<PropertyGroup, Set<GenericRelation>>();

			for (GenericRelation relation : relationGroup) {
				String propertyString = buildPropertyString(relation);
				PropertyGroup propertyGroup = propertyGroups.get(propertyString);

				// create a new property group that only contains the property
				// itself (to group identical relations from different sources)
				if (propertyGroup == null) {
					Set<String> properties = new HashSet<String>();
					properties.add(propertyString);
					if (relation.getDataSet().getSource() == Source.DBPEDIA) {
						for (Language language : languages) {
							properties.add(buildPropertyString(relation,
									DataSets.getInstance().getDataSet(language, Source.DBPEDIA)));
						}
					}
					propertyGroup = new PropertyGroup(properties);
					for (String property : properties)
						propertyGroups.put(property, propertyGroup);
				}

				if (propertyGroup != null) {
					if (!relationsByPropertyGroup.containsKey(propertyGroup))
						relationsByPropertyGroup.put(propertyGroup, new HashSet<GenericRelation>());
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

	private Set<Set<GenericRelation>> buildRelationGroupCandidates() {

		System.out.println("buildRelationGroupCandidates");

		Map<Entity, Map<Entity, Set<GenericRelation>>> relationMap = new HashMap<Entity, Map<Entity, Set<GenericRelation>>>();

		Set<Set<GenericRelation>> relationGroupCandidates = new HashSet<Set<GenericRelation>>();

		for (GenericRelation relation : DataStore.getInstance().getGenericRelations()) {
			if (!relationMap.containsKey(relation.getSubject()))
				relationMap.put(relation.getSubject(), new HashMap<Entity, Set<GenericRelation>>());
			if (!relationMap.get(relation.getSubject()).containsKey(relation.getObject()))
				relationMap.get(relation.getSubject()).put(relation.getObject(), new HashSet<GenericRelation>());
			relationMap.get(relation.getSubject()).get(relation.getObject()).add(relation);
		}

		for (Entity subject : relationMap.keySet()) {
			for (Entity object : relationMap.get(subject).keySet()) {
				if (relationMap.get(subject).get(object).size() > 1)
					relationGroupCandidates.add(relationMap.get(subject).get(object));
			}
		}

		return relationGroupCandidates;
	}

	// private String printDummyRelation(GenericRelation relation) {
	// String startTime = "X";
	// if (relation.getStartTime() != null)
	// startTime = dateFormat.format(relation.getStartTime().getDate());
	// String endTime = "X";
	// if (relation.getEndTime() != null)
	// endTime = dateFormat.format(relation.getEndTime().getDate());
	//
	// return relation.getSubject().getWikidataId() + "-" +
	// relation.getProperty() + "-" + relation.getObject().getId()
	// + "-" + startTime + "-" + endTime;
	// }

	private String buildPropertyString(GenericRelation relation) {
		return relation.getDataSet().getId() + ":" + relation.getProperty();
	}

	private String buildPropertyString(GenericRelation relation, DataSet dataSet) {
		return dataSet.getId() + ":" + relation.getProperty();
	}

	private void integrateRelationGroups(Set<Set<GenericRelation>> relationGroups) {

		System.out.println("Groups of relations that can be merged: " + relationGroups.size());

		// for (Set<GenericRelation> relationGroup : relationGroups) {
		// System.out.println("GROUP X");
		// for (GenericRelation rel : relationGroup)
		// System.out.println(printDummyRelation(rel));
		// System.out.println("");
		// }

		int numberOfExamples = 3;

		for (Set<GenericRelation> relationGroup : relationGroups) {

			// TODO: update DataStore.getInstance().getGenericRelations()

			GenericRelation newRelation = new GenericRelation();

			if (numberOfExamples > 0)
				System.out.println("Relation Group:");

			// all relations have the same subject and object
			for (GenericRelation relation : relationGroup) {

				if (numberOfExamples > 0) {
					System.out.println(" " + relation.getSubject().getWikidataId() + "\t" + relation.getProperty()
							+ "\t" + relation.getObject().getWikidataId());
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
			for (GenericRelation relation : relationGroup) {
				newRelation.getProperties()
						.add(new SubProperty(relation.getProperty(), relation.getDataSet(), relation.getPrefix()));
			}

			// remove old relations
			DataStore.getInstance().getGenericRelations().removeAll(relationGroup);
			// add new merged relation
			DataStore.getInstance().getGenericRelations().add(newRelation);

			if (numberOfExamples > 0)
				System.out.println("");

			numberOfExamples -= 1;

			relationGroup = null;
		}

		relationGroups = null;
	}

}
