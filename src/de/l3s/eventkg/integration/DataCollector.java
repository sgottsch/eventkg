package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings.TemporalPropertyType;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class DataCollector extends Extractor {

	private Set<Event> uniqueEvents = new HashSet<Event>();

	/**
	 * Set of entities which may never be interpreted as events.
	 */
	private Set<Entity> blacklistEvents = new HashSet<Entity>();

	private WikidataIdMappings wikidataIdMappings;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);

		Config.init("config_eventkb_local.txt");

		DataCollector dc = new DataCollector(languages);
		dc.init();
		dc.collectEvents();
		dc.collectTimes();
	}

	public DataCollector(List<Language> languages) {
		super("DataCollector", de.l3s.eventkg.meta.Source.ALL, "?.", languages);
	}

	public void run(String[] args) {
		System.out.println("Load Wikidata ID mapping.");
		init();
		System.out.println("Collect event pages.");
		collectEvents();
		System.out.println("Collect \"part of\" relations.");
		collectPartOfs();
		System.out.println("Collect \"follows\" relations.");
		collectPreviousEvents();
		System.out.println("Collect \"followed by\" relations.");
		collectNextEvents();
		System.out.println("Collect event times.");
		collectTimes();
		System.out.println("Collect event locations.");
		collectLocations();
		// System.out.println("Example.");
		// dataCollector.test("German_intervention_against_ISIL");
	}

	private void init() {
		this.wikidataIdMappings = new WikidataIdMappings(this.languages);
		wikidataIdMappings.load();
		wikidataIdMappings.loadTemporalProperties();
	}

	public void run() {
		System.out.println("Load Wikidata ID mapping.");
		init();
		System.out.println("Collect event pages.");
		collectEvents();
		System.out.println("Collect \"part of\" relations.");
		collectPartOfs();
		System.out.println("Collect event times.");
		collectTimes();
		System.out.println("Collect event locations.");
		collectLocations();
	}

	private void collectLocations() {
		collectLocationsYAGO();
		collectLocationsDBpedia();
		collectLocationsWikidata();
		writeLocationsToFile();
	}

	private void writeLocationsToFile() {
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

				String wikipedia1Label = parts[0];
				String entity2Label = parts[2];

				Event event1 = findEvent(Language.EN, wikipedia1Label);

				if (event1 != null) {
					Entity entityObject = getEntity(Language.EN, entity2Label);
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

				if (event.getWikidataId() == null) {
					// TODO
					// System.out.println("Missing Wikidata id: " +
					// event.getWikipediaLabelsString(this.languages));
					continue;
				}

				writer.write(event.getWikidataId());
				writer.write(Config.TAB);
				writer.write(event.getWikipediaLabelsString(this.languages));
				writer.write(Config.TAB);
				// TODO
				// if (event.getWikidataId() != null)
				// writer.write(event.getWikidataId());
				// else
				// writer.write("\\N");
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
	}

	private void collectNextEvents() {
		collectNextEventsWikidata();
		collectNextEventsDBpedia();
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

	private void collectTimes() {
		// Sort by trust: Wikidata with highest trust, overwrites others
		collectTimesDBpedia();
		collectTimesYAGO();
		collectTimesWikidata();

		writeDatesToFile();
	}

	private void collectTimesDBpedia() {

		for (Language language : this.languages) {

			BufferedReader br = null;
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_TIMES, language);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String line;
			try {
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipediaLabel = parts[0];
					String timeString = parts[2];

					Event event = findEvent(Language.EN, wikipediaLabel);

					if (event == null)
						continue;

					Date date;
					try {
						date = TimeTransformer.generateTimeForDBpedia(timeString);

						if (event.getStartTime() == null || event.getStartTime().after(date))
							event.setStartTime(date);
						if (event.getEndTime() == null || event.getEndTime().before(date))
							event.setEndTime(date);
					} catch (ParseException e) {
						e.printStackTrace();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeDatesToFile() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_EVENT_TIMES);
			for (Event event : uniqueEvents) {
				if (event.getStartTime() != null || event.getEndTime() != null) {
					String startTimeString = "\\N";
					if (event.getStartTime() != null)
						startTimeString = FileLoader.PARSE_DATE_FORMAT.format(event.getStartTime());
					String endTimeString = "\\N";
					if (event.getEndTime() != null)
						endTimeString = FileLoader.PARSE_DATE_FORMAT.format(event.getEndTime());

					writer.write(event.getWikidataId() + Config.TAB + event.getWikipediaLabelsString(this.languages)
							+ Config.TAB + startTimeString + Config.TAB + endTimeString + Config.NL);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void collectTimesYAGO() {
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_EVENT_TIMES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikipediaLabel = parts[0].substring(1, parts[0].length() - 1).replaceAll(" ", "_");

				Event event = createEvent(Language.EN, wikipediaLabel, "collectTimesYAGO");

				if (event == null)
					continue;

				String property = parts[1];
				String timeString = parts[2];

				try {
					Date date1 = TimeTransformer.generateEarliestTimeFromXsd(timeString);
					Date date1L = TimeTransformer.generateLatestTimeFromXsd(timeString);
					event.setStartTime(date1);

					// there are two properties only: happenedOnDate and
					// startedOnDate. If startedOnDate, leave the end time null.
					// update: added endedOnDate
					if (!property.equals("<startedOnDate>"))
						event.setEndTime(date1L);
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

	private void collectTimesWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String entityWikidataId = parts[0];

				Event event = findEventFromWikidataId(entityWikidataId);

				if (event == null)
					continue;

				String propertyWikidataId = parts[1];
				String timeString = parts[2];

				TemporalPropertyType type = wikidataIdMappings.getWikidataTemporalPropertyTypeById(propertyWikidataId);

				try {

					if (type == TemporalPropertyType.START || type == TemporalPropertyType.BOTH) {
						Date dateEarliest = TimeTransformer.generateEarliestTimeForWikidata(timeString);
						event.setStartTime(dateEarliest);
					}
					if (type == TemporalPropertyType.END || type == TemporalPropertyType.BOTH) {
						Date dateLatest = TimeTransformer.generateLatestTimeForWikidata(timeString);
						event.setEndTime(dateLatest);
					}

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

		// collectTimesWikidataStart();
		// collectTimesWikidataEnd();
		// collectTimesWikidataPoint();
	}

	// private void collectTimesWikidataStart() {
	// BufferedReader br = null;
	// try {
	// try {
	// br = FileLoader.getReader(FileName.WIKIDATA_START_TIME);
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	//
	// String[] parts = line.split("\t");
	//
	// Event event = findEventFromWikidataId(parts[0], parts[3]);
	//
	// if (event == null)
	// continue;
	//
	// String timeString = parts[2];
	//
	// try {
	// Date dateEarliest =
	// TimeTransformer.generateEarliestTimeForWikidata(timeString);
	// event.setStartTime(dateEarliest);
	// } catch (ParseException e) {
	// e.printStackTrace();
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

	// private Event findEventFromWikidataId(String wikidataId, String
	// wikipediaLabel) {
	//
	// wikipediaLabel = wikipediaLabel.replaceAll(" ", "_");
	//
	// // first find event by Wikidata id, then Wikipedia label (it
	// // could be that the Wikidata mapping was not created, because
	// // the event is found in other sources.
	// // String wikidataId = parts[0];
	// if (!wikidataIdStringMapping.containsKey(wikidataId))
	// return null;
	//
	// Event event =
	// events.get(wikidataIdStringMapping.containsKey(wikidataId));
	//
	// if (event == null) {
	// // String wikipediaLabel = parts[3].replaceAll(" ", "_");
	// event = events.get(wikipediaLabel);
	//
	// if (event == null)
	// return null;
	// }
	// return event;
	// }

	// private void collectTimesWikidataEnd() {
	// BufferedReader br = null;
	// try {
	// try {
	// br = FileLoader.getReader(FileName.WIKIDATA_END_TIME);
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	//
	// String[] parts = line.split("\t");
	//
	// Event event = findEventFromWikidataId(parts[0], parts[3]);
	//
	// if (event == null)
	// continue;
	//
	// String timeString = parts[2];
	//
	// try {
	// Date date = TimeTransformer.generateLatestTimeForWikidata(timeString);
	// event.setEndTime(date);
	// } catch (ParseException e) {
	// e.printStackTrace();
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

	// private void collectTimesWikidataPoint() {
	// BufferedReader br = null;
	// try {
	// try {
	// br = FileLoader.getReader(FileName.WIKIDATA_POINT_IN_TIME);
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	//
	// String[] parts = line.split("\t");
	//
	// Event event = findEventFromWikidataId(parts[0], parts[3]);
	//
	// if (event == null)
	// continue;
	//
	// String timeString = parts[2];
	//
	// try {
	// Date dateEarliest =
	// TimeTransformer.generateEarliestTimeForWikidata(timeString);
	// Date dateLatest =
	// TimeTransformer.generateLatestTimeForWikidata(timeString);
	// event.setStartTime(dateEarliest);
	// event.setEndTime(dateLatest);
	// } catch (ParseException e) {
	// e.printStackTrace();
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

					String wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
							.replaceAll(" ", "_");
					String wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
							.replaceAll(" ", "_");

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

					String wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
							.replaceAll(" ", "_");
					String wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
							.replaceAll(" ", "_");

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

					Event event = createEvent(language, wikiLabel, "loadDBpediaEvents");
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
					if (noEvent != null)
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

			System.out.println("Number of blacklist events extracted from DBpedia (" + language.getLanguageLowerCase()
					+ "): " + numberOfDBpediaEvents);
		}
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

		if (entity.getEventEntity() != null)
			return entity.getEventEntity();

		Event newEvent = new Event(entity);

		uniqueEvents.add(newEvent);

		return newEvent;
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

		if (entity == null)
			return null;

		if (entity.getEventEntity() != null)
			return entity.getEventEntity();

		Event newEvent = new Event(entity);
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

				createEvent(Language.EN, wikiLabel, "loadWCEEvents");
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

					Event event = createEvent(language, wikiLabel, "loadWikipediaEvents");
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

				Event event = createEventByWikidataId(parts[0], "loadWikidataEvents");
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

}
