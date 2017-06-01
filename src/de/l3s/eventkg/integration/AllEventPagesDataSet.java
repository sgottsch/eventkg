package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings.TemporalPropertyType;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.Prefix;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class AllEventPagesDataSet {

	private Map<Language, Map<String, Event>> eventsByWikipediaLabel;

	private Map<String, Event> eventsByWikidataId;

	private Set<Event> events;

	private WikidataIdMappings wikidataIdMappings;

	private List<Language> languages;

	public AllEventPagesDataSet() {
	}

	public AllEventPagesDataSet(List<Language> languages) {
		this.languages = languages;
	}

	public void load() {

		this.eventsByWikipediaLabel = new HashMap<Language, Map<String, Event>>();
		this.eventsByWikidataId = new HashMap<String, Event>();
		this.events = new HashSet<Event>();

		for (Language language : this.languages) {
			this.eventsByWikipediaLabel.put(language, new HashMap<String, Event>());
		}

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
				// if (wikidataId.equals("\\N"))
				// wikidataId = null;

				Entity entity = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);

				if (entity == null) {
					// this happens in case of "list" entities that were removed
					// afterwards
					continue;
				}

				Event event = new Event(entity);
				events.add(event);
				DataStore.getInstance().addEvent(event);

				for (Language language : entity.getWikipediaLabels().keySet()) {
					if (entity.getWikipediaLabel(language) != null)
						eventsByWikipediaLabel.get(language).put(entity.getWikipediaLabel(language), event);
				}
				eventsByWikidataId.put(wikidataId, event);
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

		System.out.println("loadEventTimes");
		loadEventTimes();
		System.out.println("loadEventLocations");
		loadEventLocations();
		System.out.println("loadSubEvents");
		loadSubEvent();
	}

	private void loadEventTimes() {
		collectTimesYAGO();
		collectTimesWikidata();
		loadEventTimesIntegrated();
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

				Event event = getEventByWikipediaLabel(Language.EN, wikipediaLabel);

				if (event == null)
					continue;

				String property = parts[1];
				String timeString = parts[2];

				try {
					Date date1 = TimeTransformer.generateEarliestTimeFromXsd(timeString);
					Date date1L = TimeTransformer.generateLatestTimeFromXsd(timeString);
					event.setStartTime(date1);

					DataStore.getInstance().addStartTime(
							new StartTime(event, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO), date1));

					// there are two properties only: happenedOnDate and
					// startedOnDate. If startedOnDate, leave the end time null.
					// update: Added endedOnDate
					if (!property.equals("<startedOnDate>")) {
						DataStore.getInstance().addEndTime(new EndTime(event,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO), date1L));
						event.setEndTime(date1L);
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

				Event event = getEventByWikidataId(entityWikidataId);

				if (event == null)
					continue;

				String propertyWikidataId = parts[1];
				String timeString = parts[2];

				TemporalPropertyType type = wikidataIdMappings.getWikidataTemporalPropertyTypeById(propertyWikidataId);

				try {

					if (type == TemporalPropertyType.START || type == TemporalPropertyType.BOTH) {
						Date dateEarliest = TimeTransformer.generateEarliestTimeForWikidata(timeString);
						event.setStartTime(dateEarliest);
						DataStore.getInstance().addStartTime(new StartTime(event,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), dateEarliest));
					}
					if (type == TemporalPropertyType.END || type == TemporalPropertyType.BOTH) {
						Date dateLatest = TimeTransformer.generateLatestTimeForWikidata(timeString);
						event.setEndTime(dateLatest);
						DataStore.getInstance().addEndTime(new EndTime(event,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), dateLatest));
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

	private void loadEventTimesIntegrated() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_EVENT_TIMES);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId = parts[0];

				Event event = getEventByWikidataId(wikidataId);

				if (event == null) {
					continue;
				}

				if (!parts[2].equals("\\N")) {
					try {
						Date startTime = FileLoader.PARSE_DATE_FORMAT.parse(parts[2]);
						// DataStore.getInstance().addStartTime(new
						// StartTime(event, null, null, startTime));
						event.setStartTime(startTime);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				if (!parts[3].equals("\\N")) {
					try {
						Date endTime = FileLoader.PARSE_DATE_FORMAT.parse(parts[3]);
						event.setEndTime(endTime);
						// DataStore.getInstance().addEndTime(new EndTime(event,
						// null, null, endTime));
					} catch (ParseException e) {
						e.printStackTrace();
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

	private void loadSubEvent() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_PART_OF_RELATIONS);

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

				GenericRelation relation = new GenericRelation(event2, dataSet, Prefix.SCHEMA_ORG, "subEvent", event1,
						null);
				DataStore.getInstance().addGenericRelation(relation);

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
		return eventsByWikipediaLabel.get(language).get(wikipediaLabel);
	}

	public Event getEventByWikidataId(String wikidataId) {
		return eventsByWikidataId.get(wikidataId);
	}

	public Set<String> getWikidataIdsOfAllEvents() {
		return this.eventsByWikidataId.keySet();
	}

	public void init() {
		this.wikidataIdMappings = new WikidataIdMappings(languages);
		this.wikidataIdMappings.load();
		load();
	}

	public WikidataIdMappings getWikidataIdMappings() {
		return wikidataIdMappings;
	}

	public Set<Event> getEvents() {
		return events;
	}

	public void setEvents(Set<Event> events) {
		this.events = events;
	}

}
