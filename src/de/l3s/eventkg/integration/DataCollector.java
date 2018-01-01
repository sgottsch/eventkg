package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.dbpedia.DBpediaAllLocationsLoader;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import edu.stanford.nlp.util.StringUtils;

public class DataCollector extends Extractor {

	private Set<Event> uniqueEvents = new HashSet<Event>();

	/**
	 * Set of entities which may never be interpreted as events.
	 */
	private Set<Entity> blacklistEvents = new HashSet<Entity>();

	private WikidataIdMappings wikidataIdMappings;

	private Set<Entity> locations;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);

		Config.init("config_eventkb_local.txt");

		DataCollector dc = new DataCollector(languages);
		dc.init();
		dc.collectEvents();
	}

	public DataCollector(List<Language> languages) {
		super("DataCollector", de.l3s.eventkg.meta.Source.ALL, "?.", languages);
	}

	public void run(String[] args) {
		System.out.println("Load Wikidata ID mapping.");
		init();
		System.out.println("Collect entity sub/parent locations.");
		collectSubLocations();
		System.out.println("Collect event pages.");
		collectEvents();
		System.out.println("Collect \"part of\" relations.");
		collectPartOfs();
		System.out.println("Collect \"follows\" relations.");
		collectPreviousEvents();
		System.out.println("Collect \"followed by\" relations.");
		collectNextEvents();
		System.out.println("Collect event locations.");
		collectEventLocations();
		// System.out.println("Collect entities with existence times.");
		// collectEntitiesWithExistenceTimes();
	}

	// private void collectEntitiesWithExistenceTimes() {
	//
	// for (StartTime startTime : DataStore.getInstance().getStartTimes()) {
	// this.entitiesWithExistenceTime.add(startTime.getSubject());
	// }
	// for (EndTime endTime : DataStore.getInstance().getEndTimes()) {
	// this.entitiesWithExistenceTime.add(endTime.getSubject());
	// }
	//
	// PrintWriter writer = null;
	// try {
	// writer =
	// FileLoader.getWriter(FileName.ALL_ENTITIES_WITH_EXISTENCE_TIMES);
	//
	// for (Entity entity : entitiesWithExistenceTime) {
	//
	// if (entity.getWikidataId() == null)
	// continue;
	//
	// writer.write(entity.getWikidataId());
	// writer.write(Config.TAB);
	// writer.write(entity.getWikipediaLabelsString(this.languages));
	// writer.write(Config.TAB);
	// writer.write(Config.NL);
	// }
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } finally {
	// writer.close();
	// }
	//
	// }

	private void init() {
		this.wikidataIdMappings = new WikidataIdMappings(this.languages);
		wikidataIdMappings.load();
		wikidataIdMappings.loadTemporalProperties();
	}

	public void run() {
		System.out.println("Load Wikidata ID mapping.");
		init();
		System.out.println("Collect entity sub/parent locations.");
		collectSubLocations();
		System.out.println("Collect event pages.");
		collectEvents();
		System.out.println("Collect \"part of\" relations.");
		collectPartOfs();
		System.out.println("Collect \"follows\" relations.");
		collectPreviousEvents();
		System.out.println("Collect \"followed by\" relations.");
		collectNextEvents();
		System.out.println("Collect event locations.");
		collectEventLocations();
	}

	private void collectEventLocations() {
		collectLocationsYAGO();
		collectLocationsDBpedia();
		collectLocationsWikidata();
		writeLocationsToFile();
	}

	private void collectSubLocations() {
		collectSubLocationsWikidata();
		minimizeSubLocations();
		writeSubLocationsToFile();
	}

	private void writeSubLocationsToFile() {

		System.out.println("writeSubLocationsToFile");

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_SUB_LOCATIONS);
			for (Entity subLocation : this.locations) {
				for (Entity location : subLocation.getParentLocations()) {
					writer.write(location.getWikidataId());
					writer.write(Config.TAB);
					writer.write(location.getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(subLocation.getWikidataId());
					writer.write(Config.TAB);
					writer.write(subLocation.getWikipediaLabelsString(this.languages));
					writer.write(Config.NL);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeLocationsToFile() {

		System.out.println("writeLocationsToFile");

		System.out.println("uniqueEvents: " + uniqueEvents.size());

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_LOCATIONS);
			for (Event event : uniqueEvents) {
				for (Entity location : event.getLocationsWithDataSets().keySet()) {
					for (DataSet dataSet : event.getLocationsWithDataSets().get(location)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(location.getWikidataId());
						writer.write(Config.TAB);
						writer.write(location.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void collectLocationsWikidata() {

		System.out.println("collectLocationsWikidata");

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				String entity2WikidataId = parts[2];

				Event event1 = findEventFromWikidataId(parts[0]);

				if (event1 != null) {
					Entity entityObject = getEntityFromWikidataId(entity2WikidataId);
					if (entityObject != null)
						event1.addLocation(entityObject,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
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

	private void collectLocationsDBpedia() {

		for (Language language : this.languages) {

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_EVENT_LOCATIONS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = parts[0];
					String entity2Label = parts[2];

					Event event1 = findEvent(language, wikipedia1Label);
					if (event1 != null) {
						Entity entityObject = getEntity(language, entity2Label);
						if (entityObject != null)
							event1.addLocation(entityObject,
									DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
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

	private void collectLocationsYAGO() {
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
				yagoLabelExtractor1.extractLabel();
				if (!yagoLabelExtractor1.isValid())
					continue;

				YAGOLabelExtractor yagoLabelExtractor2 = new YAGOLabelExtractor(parts[2], this.languages);
				yagoLabelExtractor2.extractLabel();
				if (!yagoLabelExtractor2.isValid())
					continue;

				Event event1 = findEvent(yagoLabelExtractor1.getLanguage(), yagoLabelExtractor1.getWikipediaLabel());

				if (event1 != null) {
					Entity entityObject = getEntity(yagoLabelExtractor2.getLanguage(),
							yagoLabelExtractor2.getWikipediaLabel());
					if (entityObject != null)
						event1.addLocation(entityObject, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
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

	// private void test(String wikipediaLabel) {
	// wikipediaLabel = wikipediaLabel.replaceAll(" ", "_");
	// Event event = events.get(wikipediaLabel);
	//
	// System.out.println(event.getOneLineRepresentation());
	// for (Entity location : event.getLocations())
	// System.out.println("\tLocation: " + location.getWikipediaLabel());
	//
	// if (!event.getChildren().isEmpty()) {
	// System.out.println("Children:");
	// printChildren(event, "\t");
	// }
	//
	// if (!event.getParents().isEmpty()) {
	// System.out.println("Parents:");
	// printParents(event, "\t");
	// }
	//
	// }
	//
	// private void printParents(Event event, String indent) {
	// for (Event parent : event.getParents()) {
	// if (parent.getStartTime() != null || parent.getEndTime() != null)
	// System.out.println(indent + parent.getOneLineRepresentation());
	// printParents(parent, indent + "\t");
	// }
	// }
	//
	// private void printChildren(Event event, String indent) {
	// for (Event child : event.getChildren()) {
	// if (child.getStartTime() != null || child.getEndTime() != null)
	// System.out.println(indent + child.getOneLineRepresentation());
	// printChildren(child, indent + "\t");
	// }
	// }

	private void collectEvents() {

		// Load event blacklists first, so they can be ignored later
		System.out.println("loadDBpediaBlacklistEvents.");
		loadDBpediaBlacklistEvents();
		System.out.println("loadLocationsBlacklistEvents.");
		loadLocationsBlacklistEvents();
		System.out.println("loadWikidataBlacklistEvents.");
		loadWikidataBlacklistEvents();

		System.out.println("loadWikidataEvents.");
		loadWikidataEvents();
		System.out.println("loadDBpediaEvents.");
		loadDBpediaEvents();
		System.out.println("loadWikipediaEvents.");
		loadWikipediaEvents();

		// TODO: Reintegrate WCE events? Has errors like "Spain" and "London".
		// loadWCEEvents();

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_EVENT_PAGES);
			for (Event event : uniqueEvents) {

				if (event.getWikidataId() == null)
					continue;

				writer.write(event.getWikidataId());
				writer.write(Config.TAB);
				writer.write(event.getWikipediaLabelsString(this.languages));
				writer.write(Config.TAB);
				writer.write(StringUtils.join(event.getEventInstanceComments(), " | "));
				writer.write(Config.TAB);

				writer.write(Config.NL);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void collectPartOfs() {
		collectPartOfsWikidata();
		collectPartOfsDBpedia();
		writePartOfsToFile();
	}

	private void collectPreviousEvents() {
		collectPreviousEventsWikidata();
		collectPreviousEventsDBpedia();
		writePreviousEventsToFile();
	}

	private void writePreviousEventsToFile() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_PREVIOUS_EVENTS);
			for (Event event : uniqueEvents) {
				for (Event previousEvent : event.getPreviousEvents()) {
					for (DataSet dataSet : event.getPreviousEventsWithDataSets().get(previousEvent)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(previousEvent.getWikidataId());
						writer.write(Config.TAB);
						writer.write(previousEvent.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void writeNextEventsToFile() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_NEXT_EVENTS);
			for (Event event : uniqueEvents) {
				for (Event nextEvent : event.getNextEvents()) {
					for (DataSet dataSet : event.getNextEventsWithDataSets().get(nextEvent)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(nextEvent.getWikidataId());
						writer.write(Config.TAB);
						writer.write(nextEvent.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void collectNextEvents() {
		collectNextEventsWikidata();
		collectNextEventsDBpedia();
		writeNextEventsToFile();
	}

	private void writePartOfsToFile() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_PART_OF_RELATIONS);
			for (Event event : uniqueEvents) {
				for (Event child : event.getChildrenWithDataSets().keySet()) {
					for (DataSet dataSet : event.getChildrenWithDataSets().get(child)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(child.getWikidataId());
						writer.write(Config.TAB);
						writer.write(child.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private Event findEventFromWikidataId(String wikidataId) {

		Entity entity = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		if (entity.getEventEntity() != null)
			return entity.getEventEntity();

		return null;
	}

	private void collectPartOfsDBpedia() {

		for (Language language : this.languages) {

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_EVENT_PARTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
							.replaceAll(" ", "_");
					String wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
							.replaceAll(" ", "_");

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addParent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addChild(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
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

	private void collectPreviousEventsDBpedia() {

		for (Language language : this.languages) {
			
			System.out.println("collectPreviousEventsDBpedia "+language);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_PREVIOUS_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = null;
					String wikipedia2Label = null;

					try {
						wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
								.replaceAll(" ", "_");
						wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
								.replaceAll(" ", "_");
					} catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
						System.out.println("Error, previous event: " + line);
						continue;
					}

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addPreviousEvent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addNextEvent(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
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

	private void collectNextEventsDBpedia() {

		for (Language language : this.languages) {
			
			System.out.println("collectNextEventsDBpedia "+language);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_NEXT_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = null;
					String wikipedia2Label = null;

					try {
						wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
								.replaceAll(" ", "_");
						wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
								.replaceAll(" ", "_");
					} catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
						System.out.println("Error, next event: " + line);
						continue;
					}

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addNextEvent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addPreviousEvent(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
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

	private void collectPartOfsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_PART_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addParent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addChild(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

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

	private void collectNextEventsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_FOLLOWED_BY);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addNextEvent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addPreviousEvent(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

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

	private void collectPreviousEventsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_FOLLOWS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addPreviousEvent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addNextEvent(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

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

	private void loadDBpediaEvents() {

		for (Language language : this.languages) {

			int numberOfDBpediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_EVENTS_FILE_NAME, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					// Fix some errors in the data where the entity label is
					// e.g.
					// "Netherlands_at_the_1924_Summer_Olympics__June_9,_1924__1",
					// which should just be
					// "Netherlands_at_the_1924_Summer_Olympics"
					if (wikiLabel.contains("__"))
						continue;
					// wikiLabel = wikiLabel.substring(0,
					// wikiLabel.indexOf("__"));

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Event event = createEvent(language, wikiLabel,
							"DBpedia, " + language + " (" + line.split(Config.TAB)[2] + ")");
					if (event != null)
						numberOfDBpediaEvents += 1;
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

			System.out.println("Number of events extracted from DBpedia (" + language.getLanguageLowerCase() + "): "
					+ numberOfDBpediaEvents);
		}

	}

	private void loadDBpediaBlacklistEvents() {
		for (Language language : this.languages) {

			int numberOfDBpediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_NO_EVENTS_FILE_NAME, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					// Fix some errors in the data where the entity label is
					// e.g.
					// "Netherlands_at_the_1924_Summer_Olympics__June_9,_1924__1",
					// which should just be
					// "Netherlands_at_the_1924_Summer_Olympics"
					if (wikiLabel.contains("__"))
						continue;
					// wikiLabel = wikiLabel.substring(0,
					// wikiLabel.indexOf("__"));

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Entity noEvent = createBlacklistEvent(language, wikiLabel, "loadDBpediaBlacklistEvents");
					if (noEvent != null) {
						numberOfDBpediaEvents += 1;
						if (noEvent.getWikidataId().equals(("Q362")))
							System.out.println("loadDBpediaBlacklistEvents: " + line);
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

			System.out.println("Number of blacklist events extracted from DBpedia (" + language.getLanguageLowerCase()
					+ "): " + numberOfDBpediaEvents);
		}
	}

	private void loadLocationsBlacklistEvents() {

		// Locations may never be events.

		Set<String> wikidataBlackListEvents = new HashSet<String>();

		for (Language language : this.languages) {
			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_ALL_LOCATIONS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {
					createBlacklistEvent(language, line, "loadLocationsBlacklistEvents");

					wikidataBlackListEvents.add(line);
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

		System.out.println(
				"Number of blacklist events extracted from DBpedia locations : " + wikidataBlackListEvents.size());
	}

	private void loadWikidataBlacklistEvents() {

		// Locations may never be events.

		Set<String> wikidataBlackListEvents = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_NO_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				createBlacklistEventByWikidataID(parts[0], "loadWikidataBlacklistEvents");
				wikidataBlackListEvents.add(parts[0]);
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

		System.out.println("Number of blacklist events extracted from Wikidata : " + wikidataBlackListEvents.size());
	}

	private Event createEvent(Language language, String wikipediaLabel, String comment) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		// ignore if it's a blacklist event
		if (blacklistEvents.contains(entity))
			return null;

		if (entity.getEventEntity() != null) {
			if (comment != null)
				entity.getEventEntity().addEventInstanceComment(comment);
			return entity.getEventEntity();
		}

		Event newEvent = new Event(entity);

		if (comment != null)
			newEvent.addEventInstanceComment(comment);

		uniqueEvents.add(newEvent);

		return newEvent;
	}

	private Entity createBlacklistEventByWikidataID(String wikidataId, String comment) {

		Entity entity = getEntityFromWikidataId(wikidataId);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		if (entity.getWikidataId().equals(("Q362")))
			System.out.println("createBlacklistEventByWikidataID: " + wikidataId + " | " + comment);

		blacklistEvents.add(entity);

		return entity;
	}

	private Entity createBlacklistEvent(Language language, String wikipediaLabel, String comment) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		blacklistEvents.add(entity);

		return entity;
	}

	private Event findEvent(Language language, String wikipediaLabel) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		if (entity.getEventEntity() != null)
			return entity.getEventEntity();

		return null;
	}

	private Event createEventByWikidataId(String wikidataId, String comment) {

		Entity entity = getEntityFromWikidataId(wikidataId);

		// if (entity == null &&
		// this.wikidataIdMappings.getWikidataIdsThatHaveLabels().contains(wikidataId))
		// {
		// if (wikidataId.equals("Q17703499"))
		// System.out.println("c Found " + wikidataId + ", " + comment + ". " +
		// entity);
		// // Create entity without Wikipedia label, but any label in one of the
		// languages
		// if (entity == null) {
		// entity = new Entity(null, null, wikidataId);
		// wikidataIdMappings.getEntitiesByWikidataIds().put(wikidataId,
		// entity);
		// }
		// }

		if (blacklistEvents.contains(entity))
			return null;

		if (entity == null)
			return null;

		if (entity.getEventEntity() != null) {
			if (comment != null)
				entity.getEventEntity().addEventInstanceComment(comment);
			return entity.getEventEntity();
		}

		Event newEvent = new Event(entity);

		if (comment != null)
			newEvent.addEventInstanceComment(comment);

		uniqueEvents.add(newEvent);

		return newEvent;
	}

	private Entity getEntity(Language language, String wikipediaLabel) {
		return this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);
	}

	private Entity getEntityFromWikidataId(String wikidataId) {
		return this.wikidataIdMappings.getEntityByWikidataId(wikidataId);
	}

	@SuppressWarnings("unused")
	private void loadWCEEvents() {
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WCE_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String wikiLabel = line;

				wikiLabel = wikiLabel.replaceAll(" ", "_");

				createEvent(Language.EN, wikiLabel, "WCE event");
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

	private void loadWikipediaEvents() {

		for (Language language : this.languages) {

			int numberOfWikipediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.WIKIPEDIA_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Event event = createEvent(language, wikiLabel,
							"Wikipedia, " + language + " (" + line.split(Config.TAB)[1] + ")");
					if (event != null)
						numberOfWikipediaEvents += 1;
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

			System.out.println("Number of events extracted from Wikipedia (" + language.getLanguageLowerCase() + "): "
					+ numberOfWikipediaEvents);
		}

	}

	private void loadWikidataEvents() {

		int numberOfWikidataEvents = 0;

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);
				String wikiLabel = parts[2].replaceAll(" ", "_");

				// TODO: Strict restriction
				// if (wikiLabel.equals("\\N"))
				// continue;

				wikiLabel = wikiLabel.replaceAll(" ", "_");
				if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
					continue;

				Event event = createEventByWikidataId(parts[0], "Wikidata (" + line.split(Config.TAB)[3] + ")");

				if (event != null)
					numberOfWikidataEvents += 1;
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

		System.out.println("Number of events extracted from Wikidata: " + numberOfWikidataEvents);
	}

	private void collectSubLocationsWikidata() {

		System.out.println("collectSubLocationsWikidata");

		this.locations = DBpediaAllLocationsLoader.loadLocationEntities(this.languages, this.wikidataIdMappings);

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				String type = parts[0];
				String entity1WikidataId = parts[1];
				String entity2WikidataId = parts[3];

				Entity location1 = getEntityFromWikidataId(entity1WikidataId);

				if (location1 == null) {
					// entity not found (no name in any of the given langauges)
					continue;
				}

				Entity location2 = getEntityFromWikidataId(entity2WikidataId);
				if (location2 == null) {
					// entity not found (no name in any of the given langauges)
					continue;
				}

				if (location1 == location2)
					continue;

				if (location1.isLocation() && location2.isLocation()) {
					if (type.equals(Config.SUB_LOCATION_SYMBOL)) {
						location1.addParentLocation(location2);
						location2.addSubLocation(location1);
					} else if (type.equals(Config.PARENT_LOCATION_SYMBOL)) {
						location2.addParentLocation(location1);
						location1.addSubLocation(location2);
					}
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

	private void minimizeSubLocations() {

		System.out.println("minimizeSubLocations");

		// transitive parents
		for (Entity location : this.locations) {
			collectAllParents(location, location.getParentLocations());
			// no self loops!
			location.getAllParentLocations().remove(location);
		}

		// if we have
		// Brandenburger Tor, parentLocation: Berlin
		// and
		// Brandenburger Tor, parentLocation: Germany
		// and
		// Berlin, parenLocation: Germany
		// only keep row 1 & 3

		for (Entity startLocation : this.locations) {
			// union of all the parent locations of each location
			Set<Entity> allParentLocations = new HashSet<Entity>();
			for (Entity parentLocation : startLocation.getParentLocations())
				allParentLocations.addAll(parentLocation.getAllParentLocations());

			// only keep those locations which are not in that union
			startLocation.getParentLocations().removeAll(allParentLocations);
		}

		// // use symmetry for sub locations
		// for (Entity startLocation : this.locations) {
		// for (Entity parent : startLocation.getParentLocations()) {
		// parent.addSubLocation(startLocation);
		// }
		// }
	}

	public static void collectAllParents(Entity location, Set<Entity> parents) {

		for (Entity parent : parents) {
			if (!location.getAllParentLocations().contains(parent)) {
				location.addAllParentLocation(parent);
				collectAllParents(location, parent.getAllParentLocations());
			}
		}

	}

}
