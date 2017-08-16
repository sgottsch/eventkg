package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Relation;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Prefix;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class TemporalRelationsCollector extends Extractor {

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		TemporalRelationsCollector extr = new TemporalRelationsCollector(languages, allEventPagesDataSet);
		extr.run();
	}

	public TemporalRelationsCollector(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("TemporalRelationsCollector", Source.ALL, "?", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	private AllEventPagesDataSet allEventPagesDataSet;
	private Set<Relation> relations = new HashSet<Relation>();

	public void run() {
		System.out.println("Load YAGO.");
		loadYAGO();
		System.out.println("Load Wikidata.");
		loadWikidata();
		System.out.println("Load DBpedia.");
		loadDBpedia();
		System.out.println("Collect triples.");
		writeToFiles();
	}

	private void writeToFiles() {
		// try {
		// PrintWriter writer =
		// FileLoader.getWriter(FileName.ALL_TEMPORAL_RELATIONS);

		Set<String> wikidataRelationsWhoseLabelsWereStored = new HashSet<String>();

		for (Relation relation : relations) {

			Prefix prefix = null;

			// TODO: Find better solution
			switch (relation.getSource()) {
			case WIKIDATA:
				prefix = Prefix.WIKIDATA_PROPERTY;
				break;
			case YAGO:
				prefix = Prefix.YAGO;
				break;
			case DBPEDIA: {
				prefix = Prefix.DBPEDIA_ONTOLOGY;

				// TODO: Maybe add the DBpedia "Infobox Properties Mapped"
				// file to get language-specific properties.

				// switch (relation.getSourceLanguage()) {
				// case EN:
				// prefix = Prefix.DBPEDIA_ONTOLOGY;
				// break;
				// case DE:
				// prefix = Prefix.DBPEDIA_PROPERTY_DE;
				// break;
				// case PT:
				// prefix = Prefix.DBPEDIA_PROPERTY_PT;
				// break;
				// case RU:
				// prefix = Prefix.DBPEDIA_PROPERTY_RU;
				// break;
				// case FR:
				// prefix = Prefix.DBPEDIA_PROPERTY_FR;
				// break;
				// default:
				// break;
				// }

				break;
			}
			default:
				System.out.println("No prefix for " + relation.getSource() + ": "
						+ relation.getEntity1().getWikidataId() + " " + relation.getProperty());
				break;
			}

			if (relation.getProperty().startsWith("rdf-schema#")) {
				prefix = Prefix.RDFS;
				relation.setProperty(relation.getProperty().substring(relation.getProperty().indexOf("#") + 1));
			} else if (relation.getProperty().startsWith("owl#")) {
				prefix = Prefix.OWL;
				relation.setProperty(relation.getProperty().substring(relation.getProperty().indexOf("#") + 1));
			}

			GenericRelation genericRelation = new GenericRelation(relation.getEntity1(),
					DataSets.getInstance().getDataSet(relation.getSourceLanguage(), relation.getSource()), prefix,
					relation.getProperty(), relation.getEntity2(), null);
			genericRelation.setStartTime(relation.getStartTime());
			genericRelation.setEndTime(relation.getEndTime());

			if (genericRelation.getDataSet() == null)
				System.out.println("NO DATASET: " + relation.getSourceLanguage() + " " + relation.getSource());
			DataStore.getInstance().addGenericRelation(genericRelation);

			// Wikidata: Collect property labels
			if (relation.getSource() == Source.WIKIDATA) {
				if (!wikidataRelationsWhoseLabelsWereStored.contains(relation.getProperty())) {
					for (Language language : this.languages) {
						if (allEventPagesDataSet.getWikidataIdMappings().getWikidataPropertysByID(language,
								relation.getProperty()) != null) {
							DataStore.getInstance()
									.addPropertyLabel(new PropertyLabel(prefix, relation.getProperty(),
											allEventPagesDataSet.getWikidataIdMappings()
													.getWikidataPropertysByID(language, relation.getProperty()),
											language, genericRelation.getDataSet()));
						}
					}
					wikidataRelationsWhoseLabelsWereStored.add(relation.getProperty());
				}
			}
		}

	}

	private void loadYAGO() {
		loadYAGOTemporalFacts();
		loadYAGOEventRelations();
	}

	private void loadYAGOEventRelations() {
		FileName fileName = FileName.YAGO_EVENT_FACTS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;

			Relation previousRelation = null;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					String timeString = parts[2];

					if (parts[1].equals("<occursSince>")) {
						Date date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						Date date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entityLabel1 = parts[0];
					String entityLabel2 = parts[2];
					String property = parts[1];

					// remove starting end ending angle bracket from YAGO
					// property
					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN);
				}

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

	private void loadYAGOTemporalFacts() {
		FileName fileName = FileName.YAGO_TEMPORAL_FACTS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;

			Relation previousRelation = null;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					String timeString = parts[2];

					if (parts[1].equals("<occursSince>")) {
						Date date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						Date date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entityLabel1 = parts[0];
					String entityLabel2 = parts[2];
					String property = parts[1];

					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN);
				}

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

	private void loadWikidata() {
		loadWikidataTemporalRelations();
		loadWikidataAtemporalRelations();
	}

	private void loadWikidataTemporalRelations() {
		FileName fileName = FileName.WIKIDATA_TEMPORAL_FACTS;
		BufferedReader br = null;
		Set<String> props = new HashSet<String>();
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			Relation previousRelation = null;
			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					if (previousRelation == null)
						continue;

					String propertyWikidataId = parts[1];
					String timeString = parts[2];

					props.add(propertyWikidataId);

					TimeSymbol type = this.allEventPagesDataSet.getWikidataIdMappings()
							.getWikidataTemporalPropertyTypeById(propertyWikidataId);

					if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {
						Date startTime;
						try {
							startTime = TimeTransformer.generateEarliestTimeForWikidata(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(startTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {
						Date endTime;
						try {
							endTime = TimeTransformer.generateLatestTimeForWikidata(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(endTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entity1WikidataId = parts[1];
					String entity2WikidataId = parts[3];
					String propertyWikidataId = parts[2];

					Entity entity1 = buildEntityByWikidataId(entity1WikidataId);

					// ignore Wikidata items without English Wikipedia label
					// if (entity1 == null ||
					// entity1.getWikipediaLabel().equals("\\N")) {
					// previousRelation = null;
					// continue;
					// }

					Entity entity2 = buildEntityByWikidataId(entity2WikidataId);
					// ignore Wikidata items without English Wikipedia label
					// if (entity2 == null ||
					// entity2.getWikipediaLabel().equals("\\N")) {
					// previousRelation = null;
					// continue;
					// }

					// String property =
					// this.allEventPagesDataSet.getWikidataIdMappings()
					// .getWikidataPropertyById(propertyWikidataId);

					previousRelation = buildRelation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA,
							Language.EN);
				}

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
		System.out.println(props);
	}

	private void loadWikidataAtemporalRelations() {
		FileName fileName = FileName.WIKIDATA_EVENT_RELATIONS;
		BufferedReader br = null;
		Set<String> props = new HashSet<String>();
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String entity1WikidataId = parts[1];
				String entity2WikidataId = parts[3];
				String propertyWikidataId = parts[2];

				Entity entity1 = buildEntityByWikidataId(entity1WikidataId);
				Entity entity2 = buildEntityByWikidataId(entity2WikidataId);

				// String property =
				// this.allEventPagesDataSet.getWikidataIdMappings()
				// .getWikidataPropertyById(propertyWikidataId);

				buildRelation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA, Language.EN);
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
		System.out.println(props);
	}

	private Entity buildEntityByWikidataId(String entityWikidataId) {
		Entity entity = this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(entityWikidataId);
		return entity;
	}

	private void loadDBpedia() {

		for (Language language : this.languages) {
			FileName fileName = FileName.DBPEDIA_EVENT_RELATIONS;
			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(fileName, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					try {
						String entityLabel1 = parts[0];
						String entityLabel2 = parts[2];

						String property = parts[1].substring(parts[1].lastIndexOf("/") + 1, parts[1].lastIndexOf(">"));

						// TODO: Do this during extraction
						if (property.equals("type") || property.equals("location") || property.equals("place"))
							continue;

						// TODO: Check before
						if (property.equals("isPartOfMilitaryConflict") || property.equals("isPartOf")
								|| property.equals("isPartOfWineRegion")
								|| property.equals("isPartOfAnatomicalStructure"))
							continue;

						Entity entity1 = buildEntity(language, entityLabel1);
						Entity entity2 = buildEntity(language, entityLabel2);

						buildRelation(entity1, entity2, null, null, property, Source.DBPEDIA, language);
					} catch (ArrayIndexOutOfBoundsException e) {
						// problems if foaf homepage
						// System.out.println("Warning: " + line);
						continue;
					}
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
	}

	@SuppressWarnings("unused")
	private void loadWCE() {
		FileName fileName = FileName.WCE_EVENT_RELATIONS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String entityLabel1 = parts[0];
				String entityLabel2 = parts[1];

				String timeString = parts[2];

				try {
					Date time = FileLoader.PARSE_DATE_FORMAT.parse(timeString);
					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					buildRelation(entity1, entity2, time, time, "related", Source.WCE, Language.EN);

				} catch (ParseException e) {
					e.printStackTrace();
				}

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

	private Entity buildEntity(Language language, String wikipediaLabel) {
		return this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, wikipediaLabel);
	}

	private Relation buildRelation(Entity entity1, Entity entity2, Date startTime, Date endTime, String property,
			Source source, Language sourceLanguage) {

		if (entity1 == null || entity2 == null)
			return null;

		if (entity1.getEventEntity() != null)
			entity1 = entity1.getEventEntity();
		if (entity2.getEventEntity() != null)
			entity2 = entity2.getEventEntity();

		Relation relation = new Relation(entity1, entity2, startTime, endTime, property, source, sourceLanguage);

		this.relations.add(relation);

		return relation;
	}

}
