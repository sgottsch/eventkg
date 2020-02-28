package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class AllEventPagesDataSet {

	// private Map<Language, Map<String, Event>> eventsByWikipediaLabel;
	private Set<Integer> eventsWikidataIds;

	private Set<Integer> entitiesWithExistenceTimeByWikidataId;
	// private Map<Language, Map<String, Entity>>
	// entitiesWithExistenceTimeByWikipediaLabel;

	// private Set<Event> events;

	private WikidataIdMappings wikidataIdMappings;

	private List<Language> languages;
	private boolean loadEntityAndEventInfo = true;

	public AllEventPagesDataSet() {
	}

	public AllEventPagesDataSet(List<Language> languages) {
		this.languages = languages;
	}

	public void load() {

		// this.eventsByWikipediaLabel = new HashMap<Language, Map<String,
		// Event>>();
		this.eventsWikidataIds = new HashSet<Integer>();
		// this.events = new HashSet<Event>();

		// for (Language language : this.languages) {
		// this.eventsByWikipediaLabel.put(language, new HashMap<String,
		// Event>());
		// }

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_EVENT_PAGES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId = parts[0];
				int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

				// if (wikidataId.equals("\\N"))
				// wikidataId = null;

				Entity entity = this.wikidataIdMappings.getEntityByWikidataId(numericWikidataId);

				if (entity == null) {
					// this happens in case of "list" entities that were removed
					// afterwards
					continue;
				}

				Event event = new Event(entity, this.wikidataIdMappings);

				// events.add(event);
				// DataStore.getInstance().addEvent(event);

				// parse sources
				// e.g. DBpedia_en (DUL.owl#Event) Wikidata (Q3001412)
				String[] dataSetsAndEventInstances = parts[2].split(" ");
				DataSet dataSet = null;
				for (int i = 0; i < dataSetsAndEventInstances.length; i++) {
					if (i % 2 == 0)
						dataSet = DataSets.getInstance().getDataSetById(dataSetsAndEventInstances[i]);
					else {
						// remove brackets
						String eventInstance = dataSetsAndEventInstances[i].substring(1,
								dataSetsAndEventInstances[i].length() - 1);
						event.addDataSetAndEventInstance(dataSet, eventInstance);
						dataSet = null;
					}
				}

				// for (Language language :
				// entity.getWikipediaLabels().keySet()) {
				// if (entity.getWikipediaLabel(language) != null)
				// eventsByWikipediaLabel.get(language).put(entity.getWikipediaLabel(language),
				// event);
				// }
				eventsWikidataIds.add(event.getNumericWikidataId());
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

		if (loadEntityAndEventInfo) {
			System.out.println("loadEventSeries");
			loadEventSeries();
			System.out.println("loadEventTimes");
			loadEventTimes();
			System.out.println("Load entities with existence times.");
			loadEntitiesWithExistenceTimes();
			System.out.println("loadEventLocations");
			loadEventLocations();
			System.out.println("loadSubEvents");
			loadSubEvents();
			System.out.println("loadPreviousEvents");
			loadPreviousEvents();
			System.out.println("loadNextEvents");
			loadNextEvents();
			System.out.println("loadPositions");
			loadPositions();
		}
	}

	private void loadEventSeries() {
		loadWikidataRecurringEvents();
	}

	private void loadWikidataRecurringEvents() {

		int numberOfWikidataEvents = 0;

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_EVENT_SERIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);
				String wikiLabel = parts[2].replaceAll(" ", "_");

				// TODO: Is this correct?
				wikiLabel = wikiLabel.replaceAll(" ", "_");
				if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
					continue;

				Entity entityTmp = getWikidataIdMappings().getEntityByWikidataId(parts[0]);
				if (entityTmp != null && entityTmp.isEvent()) {
					((Event) entityTmp).setRecurring(true);
					numberOfWikidataEvents += 1;
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

		BufferedReader br2 = null;
		try {
			try {
				br2 = FileLoader.getReader(FileName.WIKIDATA_RECURRENT_EVENT_EDITIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br2.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				Event event = getWikidataIdMappings().getEventByWikidataId(parts[0]);
				if (event != null) {
					event.setRecurring(false);
					event.setRecurrentEventEdition(true);
					numberOfWikidataEvents += 1;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Number of recurring events extracted from Wikidata: " + numberOfWikidataEvents);
	}

	private void loadEntitiesWithExistenceTimes() {
		this.entitiesWithExistenceTimeByWikidataId = new HashSet<Integer>();

		// this.entitiesWithExistenceTimeByWikipediaLabel = new
		// HashMap<Language, Map<String, Entity>>();
		// for (Language language : this.languages) {
		// this.entitiesWithExistenceTimeByWikipediaLabel.put(language, new
		// HashMap<String, Entity>());
		// }

		System.out.println("Start times: " + DataStore.getInstance().getStartTimes().size());
		for (StartTime startTime : DataStore.getInstance().getStartTimes()) {
			Entity entity = startTime.getSubject();

			this.entitiesWithExistenceTimeByWikidataId.add(entity.getNumericWikidataId());
			// for (Language language : entity.getWikipediaLabels().keySet()) {
			// if (entity.getWikipediaLabel(language) != null)
			// entitiesWithExistenceTimeByWikipediaLabel.get(language).put(entity.getWikipediaLabel(language),
			// entity);
			// }
		}

		System.out.println("End times: " + DataStore.getInstance().getEndTimes().size());
		for (EndTime endTime : DataStore.getInstance().getEndTimes()) {
			Entity entity = endTime.getSubject();

			// TODO: REMOVE
			if (entity.getWikidataId().equals("Q567") || entity.getWikidataId().equals("Q2522577")) {
				System.out.println("End time: " + entity.getWikidataId());
			}

			this.entitiesWithExistenceTimeByWikidataId.add(entity.getNumericWikidataId());
			// for (Language language : entity.getWikipediaLabels().keySet()) {
			// if (entity.getWikipediaLabel(language) != null)
			// entitiesWithExistenceTimeByWikipediaLabel.get(language).put(entity.getWikipediaLabel(language),
			// entity);
			// }
		}

		System.out.println("Entities with existence time: " + entitiesWithExistenceTimeByWikidataId.size());
	}

	private void loadEventTimes() {

		// "event.setStartTime()" is needed for the matching of textual to named
		// events. To this end, collect times by trust of the source. The last
		// one should be the most trustworthy and overwrite the others.

		collectTimesDBpedia();
		collectTimesYAGO();
		collectTimesWikidata();
		// loadEventTimesIntegrated();
	}

	private void collectTimesYAGO() {

		System.out.println("collectTimesYAGO");

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_EXISTENCE_TIMES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				YAGOLabelExtractor yagoLabelExtractor = new YAGOLabelExtractor(parts[0], this.languages);
				yagoLabelExtractor.extractLabel();
				if (!yagoLabelExtractor.isValid())
					continue;

				Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(yagoLabelExtractor.getLanguage(),
						yagoLabelExtractor.getWikipediaLabel());

				if (entity == null)
					continue;

				Event event = null;
				if (entity.isEvent()) {
					event = (Event) entity;

					// Events in YAGO very often have wrong times when the
					// "wasDestroyedOnDate" property is used. Ignore them.
					if (parts[1].equals("<wasDestroyedOnDate>"))
						continue;

				}

				// System.out.println("Event: " + event + " -");
				// if(event!=null)
				// System.out.println("not null");

				String timeString = parts[2];
				TimeSymbol type = TimeSymbol.fromString(parts[3]);

				try {
					DateWithGranularity date1 = TimeTransformer.generateEarliestTimeFromXsd(timeString);
					DateWithGranularity date1L = TimeTransformer.generateLatestTimeFromXsd(timeString);

					if (date1 != null && (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME)) {

						if (entity.isEvent()) {
							event.setStartTime(date1);
							event.addStartTime(date1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
						}

						DataStore.getInstance().addStartTime(new StartTime(entity,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO), date1));
					}

					if (date1L != null && (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME)) {

						if (entity.isEvent()) {
							event.setEndTime(date1L);
							event.addEndTime(date1L, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
						}

						DataStore.getInstance().addEndTime(new EndTime(entity,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO), date1L));
					}

				} catch (ParseException e) {
					System.err.println("Error with line: " + line);
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
		System.out.println("collectTimesWikidata");
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

				// event: happening time. entity: existence time
				Entity entity = this.wikidataIdMappings.getEntityByWikidataId(entityWikidataId);
				Event event = null;

				if (entity == null)
					continue;

				if (entity.isEvent()) {
					event = (Event) entity;
				}

				String propertyWikidataId = parts[1];
				String timeString = parts[2];

				TimeSymbol type = wikidataIdMappings.getWikidataTemporalPropertyTypeById(propertyWikidataId);

				try {

					if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {

						DateWithGranularity dateEarliest = TimeTransformer.generateEarliestTimeForWikidata(timeString);

						if (dateEarliest != null) {

							if (entity.isEvent()) {
								event.setStartTime(dateEarliest);
								event.addStartTime(dateEarliest,
										DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
							}

							DataStore.getInstance().addStartTime(new StartTime(entity,
									DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), dateEarliest));

						}
					}
					if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {
						DateWithGranularity dateLatest = TimeTransformer.generateLatestTimeForWikidata(timeString);

						if (dateLatest != null) {

							if (entity.isEvent()) {
								event.setEndTime(dateLatest);
								event.addEndTime(dateLatest,
										DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
							}

							DataStore.getInstance().addEndTime(new EndTime(entity,
									DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), dateLatest));
						}
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

					TimeSymbol type = TimeSymbol.fromString(parts[3]);

					// event: happening time. entity: existence time
					Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);
					Event event = null;

					if (entity == null)
						continue;

					if (entity.isEvent()) {
						event = (Event) entity;
					}

					DateWithGranularity date;
					try {
						date = TimeTransformer.generateTimeForDBpedia(timeString);

						if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {
							if (entity.isEvent()) {
								event.setStartTime(date);
								event.addStartTime(date, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
							}

							DataStore.getInstance().addStartTime(new StartTime(entity,
									DataSets.getInstance().getDataSet(language, Source.DBPEDIA), date));
						}
						if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {

							if (entity.isEvent()) {
								event.setEndTime(date);
								event.addEndTime(date, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
							}

							DataStore.getInstance().addEndTime(new EndTime(entity,
									DataSets.getInstance().getDataSet(language, Source.DBPEDIA), date));
						}

					} catch (ParseException e) {
						System.out.println("Error: Cannot parse " + timeString + ". Ignore.");
						// e.printStackTrace();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadEventLocations() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_LOCATIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId = parts[0];

				Event event = getEventByWikidataId(wikidataId);

				if (event == null) {
					continue;
				}

				String wikidataIdLocation = parts[2];
				Entity location = wikidataIdMappings.getEntityByWikidataId(wikidataIdLocation);

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				if (location != null)
					DataStore.getInstance().addLocation(new Location(event, dataSet, location, null));

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

	private void loadSubEvents() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_PART_OF_RELATIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Event parentEvent = getEventByWikidataId(parts[0]);
				if (parentEvent == null) {
					continue;
				}

				Event event = getEventByWikidataId(parts[2]);
				if (event == null) {
					continue;
				}

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				event.addParent(parentEvent, dataSet);
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

	private void loadPositions() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_POSITIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Entity entity = getWikidataIdMappings().getEntityByWikidataId(parts[0]);
				if (entity == null) {
					continue;
				}

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				entity.addPosition(new Position(Double.valueOf(parts[2]), Double.valueOf(parts[3])), dataSet);

				// GenericRelation relation = new GenericRelation(event2,
				// dataSet,
				// PrefixList.getInstance().getPrefix(PrefixEnum.SEM),
				// "hasSubEvent", event1, null, false);
				// DataStore.getInstance().addGenericRelation(relation);

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

	private void loadPreviousEvents() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_PREVIOUS_EVENTS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Event event1 = getEventByWikidataId(parts[0]);
				if (event1 == null) {
					continue;
				}

				Event event2 = getEventByWikidataId(parts[2]);
				if (event2 == null) {
					continue;
				}

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				event1.addPreviousEvent(event2, dataSet);

				// GenericRelation relation = new GenericRelation(event2,
				// dataSet,
				// PrefixList.getInstance().getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY),
				// "previousEvent", event1, null,
				// false);
				// DataStore.getInstance().addGenericRelation(relation);

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

	private void loadNextEvents() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_NEXT_EVENTS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Event event1 = getEventByWikidataId(parts[0]);
				if (event1 == null) {
					continue;
				}

				Event event2 = getEventByWikidataId(parts[2]);
				if (event2 == null) {
					continue;
				}

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				event1.addNextEvent(event2, dataSet);

				// GenericRelation relation = new GenericRelation(event2,
				// dataSet,
				// PrefixList.getInstance().getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY),
				// "nextEvent", event1, null,
				// false);
				// DataStore.getInstance().addGenericRelation(relation);

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

	public Event getEventByWikipediaLabel(Language language, String wikipediaLabel) {
		Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);
		if (entity != null && entity.isEvent())
			return (Event) entity;
		else
			return null;
		// return eventsByWikipediaLabel.get(language).get(wikipediaLabel);
	}

	public Event getEventByWikidataId(String wikidataId) {
		Entity entity = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);
		if (entity != null && entity.isEvent())
			return (Event) entity;
		else
			return null;

		// return eventsByWikidataId.get(wikidataId);
	}

	public Event getEventByNumericWikidataId(int wikidataId) {
		Entity entity = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);
		if (entity != null && entity.isEvent())
			return (Event) entity;
		else
			return null;
	}

	public Set<Integer> getWikidataIdsOfAllEvents() {
		return this.eventsWikidataIds;
	}

	public void init() {
		this.wikidataIdMappings = new WikidataIdMappings(languages);
		this.wikidataIdMappings.load();
		load();
		// System.out.println(
		// "EntitiesByWikidataNumericIds: " +
		// this.wikidataIdMappings.getEntitiesByWikidataNumericIds().size());
	}

	public WikidataIdMappings getWikidataIdMappings() {
		return wikidataIdMappings;
	}

	// public Set<Event> getEvents() {
	// return events;
	// }
	//
	// public void setEvents(Set<Event> events) {
	// this.events = events;
	// }

	public Set<Integer> getWikidataIdsOfEntitiesWithExistenceTime() {
		return this.entitiesWithExistenceTimeByWikidataId;
	}

	public Entity getEntityWithExistenceTimeByWikipediaLabel(Language language, String wikipediaLabel) {
		Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);
		if (entity != null && this.entitiesWithExistenceTimeByWikidataId.contains(entity.getNumericWikidataId()))
			return entity;
		return null;
	}

	// return
	// entitiesWithExistenceTimeByWikipediaLabel.get(language).get(wikipediaLabel);
	// }

	public void setLoadEntityAndEventInfo(boolean loadEntityAndEventInfo) {
		this.loadEntityAndEventInfo = loadEntityAndEventInfo;
	}

}
