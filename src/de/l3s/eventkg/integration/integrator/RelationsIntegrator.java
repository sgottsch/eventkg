package de.l3s.eventkg.integration.integrator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.SubProperty;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RelationsIntegrator extends Extractor {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private Map<String, PropertyGroup> propertyGroups = new HashMap<String, PropertyGroup>();
	private TriplesWriter dataStoreWriter;

	private PrintWriter connectedEntitiesWriter;

	public RelationsIntegrator(List<Language> languages, TriplesWriter dataStoreWriter) {
		super("RelationsIntegrator", Source.ALL, "Fuses relations from different sources.", languages);
		this.dataStoreWriter = dataStoreWriter;
	}

	public void run() {

		try {
			this.connectedEntitiesWriter = FileLoader.getWriter(FileName.CONNECTED_ENTITIES);

			// System.out.println("Integrate times by earliest and latest
			// time.");
			// integrateTimesByTime();
			System.out.println(
					"Integrate relations that share subject, object and time, and have equivalent properties.");
			loadPropertyGroups();
			integrateRelations();
			writeTriples();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			this.connectedEntitiesWriter.close();
		}

	}

	private void writeTriples() {

		for (Iterator<GenericRelation> it = DataStore.getInstance().getGenericRelations().iterator(); it.hasNext();) {

			GenericRelation relation = it.next();

			// ignore event relations here
			if (relation.getSubject().isEvent() || relation.getObject().isEvent())
				dataStoreWriter.writeGenericEventRelation(relation);
			else {
				dataStoreWriter.writeGenericEntityRelation(relation);
				writeConnectedEntities(relation);
			}

			it.remove();
		}

		DataStore.getInstance().clearGenericRelations();
	}

	private void writeConnectedEntities(GenericRelation relation) {
		this.connectedEntitiesWriter
				.println(relation.getSubject().getWikidataId() + "," + relation.getObject().getWikidataId());
	}

	public void loadPropertyGroups() {

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

	public void integrateRelations() {
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
