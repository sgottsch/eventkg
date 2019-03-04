package de.l3s.eventkg.integration.collection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Relation;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.dbpedia.DBpediaPartOfLoader;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class EventAndTemporalRelationsCollector extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;
	private Set<Relation> relations = new HashSet<Relation>();

	private Set<String> partOfProperties;

	public static void main(String[] args) {

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		EventAndTemporalRelationsCollector extr = new EventAndTemporalRelationsCollector(languages,
				allEventPagesDataSet);
		extr.run();
	}

	public EventAndTemporalRelationsCollector(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("EventAndTemporalRelationsCollector", Source.ALL,
				"Integrates temporal relations and those between events from all sources (s.t. they use the same set of entities. Different relations are not merged.).",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Load YAGO.");
		loadYAGO();
		System.out.println("Load Wikidata.");
		loadWikidata();
		System.out.println("Load DBpedia relations.");
		loadDBpediaRelations();
		System.out.println("Collect triples with temporal relations.");
		collectTriples();
	}

	private void collectTriples() {

		Set<String> wikidataRelationsWhoseLabelsWereStored = new HashSet<String>();
		for (PropertyLabel propertyLabel : DataStore.getInstance().getPropertyLabels()) {
			wikidataRelationsWhoseLabelsWereStored.add(propertyLabel.getProperty());
		}

		System.out.println("#Relations: " + relations.size() + ".");

		int relationsSize = relations.size();
		int i = 0;
		for (Iterator<Relation> it = relations.iterator(); it.hasNext();) {

			Relation relation = it.next();
			it.remove();

			if (i % 1000000 == 0)
				System.out.println(((double) i / (double) relationsSize) + " = " + i + "/" + relationsSize);

			i += 1;

			Prefix prefix = null;

			// TODO: Find better solution
			switch (relation.getSource()) {
			case WIKIDATA:
				prefix = PrefixList.getInstance().getPrefix(PrefixEnum.WIKIDATA_PROPERTY);
				break;
			case YAGO:
				prefix = PrefixList.getInstance().getPrefix(PrefixEnum.YAGO);
				break;
			case DBPEDIA: {
				prefix = PrefixList.getInstance().getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY);

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
				prefix = PrefixList.getInstance().getPrefix(PrefixEnum.RDFS);
				relation.setProperty(relation.getProperty().substring(relation.getProperty().indexOf("#") + 1));
			} else if (relation.getProperty().startsWith("owl#")) {
				prefix = PrefixList.getInstance().getPrefix(PrefixEnum.OWL);
				relation.setProperty(relation.getProperty().substring(relation.getProperty().indexOf("#") + 1));
			}

			GenericRelation genericRelation = new GenericRelation(relation.getEntity1(),
					DataSets.getInstance().getDataSet(relation.getSourceLanguage(), relation.getSource()), prefix,
					relation.getProperty(), relation.getEntity2(), null, relation.isEntityRelation());
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

		this.relations.clear();

		System.out.println("Event/Temporal relations: " + DataStore.getInstance().getGenericRelations().size());
	}

	private void loadYAGO() {
		loadYAGOTemporalFacts();
		loadYAGOEventRelations();

		// TODO: Entity relations
		loadYAGOEntityRelations();
	}

	private void loadYAGOEntityRelations() {
		loadYAGORelations(FileName.YAGO_ENTITY_FACTS, true);
	}

	private void loadYAGOEventRelations() {
		loadYAGORelations(FileName.YAGO_EVENT_FACTS, false);
	}

	private void loadYAGORelations(FileName fileName, boolean collectEntityRelations) {

		System.out.println("loadYAGORelations: " + fileName.getFileName() + ".");

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
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
					yagoLabelExtractor1.extractLabel();
					if (!yagoLabelExtractor1.isValid())
						continue;

					YAGOLabelExtractor yagoLabelExtractor2 = new YAGOLabelExtractor(parts[2], this.languages);
					yagoLabelExtractor2.extractLabel();
					if (!yagoLabelExtractor2.isValid())
						continue;

					String property = parts[1];

					// remove starting end ending angle bracket from YAGO
					// property
					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					Entity entity1 = buildEntity(yagoLabelExtractor1.getLanguage(),
							yagoLabelExtractor1.getWikipediaLabel());
					Entity entity2 = buildEntity(yagoLabelExtractor2.getLanguage(),
							yagoLabelExtractor2.getWikipediaLabel());

					// although YAGO has some entities from other languages, we
					// say the source is English.
					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN,
							collectEntityRelations);
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
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String property = parts[1];

					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
					yagoLabelExtractor1.extractLabel();
					if (!yagoLabelExtractor1.isValid())
						continue;

					YAGOLabelExtractor yagoLabelExtractor2 = new YAGOLabelExtractor(parts[2], this.languages);
					yagoLabelExtractor2.extractLabel();
					if (!yagoLabelExtractor2.isValid())
						continue;

					Entity entity1 = buildEntity(yagoLabelExtractor1.getLanguage(),
							yagoLabelExtractor1.getWikipediaLabel());
					Entity entity2 = buildEntity(yagoLabelExtractor2.getLanguage(),
							yagoLabelExtractor2.getWikipediaLabel());

					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN,
							false);
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
		loadWikidataAtemporalEventRelations();

		// TODO: Entity relations
		loadWikidataAtemporalEntityRelations();
	}

	private void loadWikidataTemporalRelations() {
		FileName fileName = FileName.WIKIDATA_TEMPORAL_FACTS;
		BufferedReader br = null;

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

					TimeSymbol type = this.allEventPagesDataSet.getWikidataIdMappings()
							.getWikidataTemporalPropertyTypeById(propertyWikidataId);

					if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {
						DateWithGranularity startTime;
						try {
							startTime = TimeTransformer.generateEarliestTimeForWikidata(timeString);
							if (previousRelation != null && startTime != null)
								previousRelation.setStartTime(startTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {
						DateWithGranularity endTime;
						try {
							endTime = TimeTransformer.generateLatestTimeForWikidata(timeString);
							if (previousRelation != null && endTime != null)
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
							Language.EN, false);
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

	private void loadWikidataAtemporalEntityRelations() {
		loadWikidataAtemporalRelations(FileName.WIKIDATA_ENTITY_RELATIONS, true);
	}

	private void loadWikidataAtemporalEventRelations() {
		loadWikidataAtemporalRelations(FileName.WIKIDATA_EVENT_RELATIONS, false);
	}

	private void loadWikidataAtemporalRelations(FileName fileName, boolean collectEntityRelations) {
		BufferedReader br = null;

		System.out.println("loadWikidataAtemporalRelations: " + fileName.getFileName() + ".");

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

				buildRelation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA, Language.EN,
						collectEntityRelations);
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

	private Entity buildEntityByWikidataId(String entityWikidataId) {
		Entity entity = this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(entityWikidataId);
		return entity;
	}

	private void loadDBpediaRelations() {
		loadDBpedia(FileName.DBPEDIA_EVENT_RELATIONS, false);

		// TODO: Entity relations
		loadDBpedia(FileName.DBPEDIA_ENTITY_RELATIONS, true);
	}

	private void loadDBpedia(FileName fileName, boolean loadEntityRelations) {

		this.partOfProperties = DBpediaPartOfLoader.loadPartOfProperties();

		Set<String> ignoredProperties = new HashSet<String>();
		for (String property : this.partOfProperties) {
			ignoredProperties.add(property.substring(property.lastIndexOf("/") + 1, property.length() - 1));
		}

		System.out.println("loadDBpedia: " + fileName.getFileName() + ".");

		for (Language language : this.languages) {

			System.out.println("Language: " + language + " (" + fileName.getFileName() + ")");

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
						if (this.partOfProperties.contains(property))
							continue;
						if (ignoredProperties.contains(property))
							continue;

						Entity entity1 = buildEntity(language, entityLabel1);
						Entity entity2 = buildEntity(language, entityLabel2);

						buildRelation(entity1, entity2, null, null, property, Source.DBPEDIA, language,
								loadEntityRelations);
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

					buildRelation(entity1, entity2, new DateWithGranularity(time, DateGranularity.DAY),
							new DateWithGranularity(time, DateGranularity.DAY), "related", Source.WCE, Language.EN,
							false);

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

	private Relation buildRelation(Entity entity1, Entity entity2, DateWithGranularity startTime,
			DateWithGranularity endTime, String property, Source source, Language sourceLanguage,
			boolean isEntityRelation) {

		if (entity1 == null || entity2 == null)
			return null;

		Relation relation = new Relation(entity1, entity2, startTime, endTime, property, source, sourceLanguage,
				isEntityRelation);

		this.relations.add(relation);

		return relation;
	}

}
