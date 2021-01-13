package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

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

				if (wikidataId.equals("Q52"))
					continue;

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
			System.out.println("Load entities with existence times.");
			loadEntitiesWithExistenceTimes();
			// System.out.println("loadEventLocations");
			// loadEventLocations();
			// System.out.println("loadSubEvents");
			// loadSubEvents();
			// System.out.println("loadPreviousEvents");
			// loadPreviousEvents();
			// System.out.println("loadNextEvents");
			// loadNextEvents();
			// System.out.println("loadPositions");
			// loadPositions();
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

		if (FileLoader.fileExists(FileName.WIKIDATA_RECURRENT_EVENT_EDITIONS)) {
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
		}

		System.out.println("Number of recurring events extracted from Wikidata: " + numberOfWikidataEvents);
	}

	private void loadEntitiesWithExistenceTimes() {
		this.entitiesWithExistenceTimeByWikidataId = new HashSet<Integer>();

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.ALL_ENTITIES_WITH_EXISTENCE_TIMES);
			while (it.hasNext()) {
				String line = it.nextLine();
				this.entitiesWithExistenceTimeByWikidataId.add(Integer.valueOf(line));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Entities with existence time: " + entitiesWithExistenceTimeByWikidataId.size());
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
