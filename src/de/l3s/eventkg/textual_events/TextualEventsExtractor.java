package de.l3s.eventkg.textual_events;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.TextualEvent;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.currentevents.EventsFromFileExtractor;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;
import de.l3s.eventkg.source.wikipedia.model.LinksToCount;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class TextualEventsExtractor extends Extractor {

	public static final boolean PUT_ENTITIES_LINKED_IN_THE_SAME_EVENT_IN_LINK_SET = false;

	private int numberOfExtractedWikiEvents = 0;

	private AllEventPagesDataSet allEventPagesDataSet;

	private Set<TextualEvent> textualEvents;
	private Map<Event, Set<TextualEvent>> eventsToTextualEvents;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);
	private SimpleDateFormat dateFormatPageTitle = new SimpleDateFormat("MMMMM_yyyy", Locale.US);

	private Map<String, Set<TextualEvent>> eventsByDates = new HashMap<String, Set<TextualEvent>>();

	private double JACCARD_THRESHOLD = 0.2;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);
		languages.add(Language.PT);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		TextualEventsExtractor extr = new TextualEventsExtractor(languages, allEventPagesDataSet);
		extr.run();
	}

	public TextualEventsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("TextualEventsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect and integrate textual events and their relations.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		extractRelations();
	}

	private void extractRelations() {

		this.textualEvents = new HashSet<TextualEvent>();
		this.eventsToTextualEvents = new HashMap<Event, Set<TextualEvent>>();

		loadTextualEventsFromWikipedia();
		loadTextualEventsFromWCE();
		System.out.println("Total number of textual events: " + this.textualEvents.size() + ".");

		mergeEvents();

		for (Description description : DataStore.getInstance().getDescriptions()) {
			if (description.getSubject() == null)
				continue;
			if (description.getSubject().isEvent()) {
				if (description.getLabel().startsWith("CEBIT")) {
					System.out.println("Found CEBIT event: " + description.getLabel());
				}
			}
		}
	}

	private void loadTextualEventsFromWCE() {

		System.out.println("Load events from the Wikipedia Current Events Portal.");

		int numberOfWCEvents = loadWCEEvents();

		System.out.println("Number of extracted events from the Wikipedia Current Events Portal: " + numberOfWCEvents);
	}

	private void loadTextualEventsFromWikipedia() {

		System.out.println("Load textual events from Wikipedia.");

		for (Language language : this.languages) {
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_TEXTUAL_EVENTS, language)) {
				processFile(child, language);
			}
		}

		System.out.println("Number of extracted Wikipedia events: " + numberOfExtractedWikiEvents + " / "
				+ this.textualEvents.size() + ".");
	}

	private void mergeEvents() {

		for (Language language : languages) {
			DataStore.getInstance().getMentionCountsFromTextualEvents().put(language,
					new HashMap<Entity, Map<Entity, Integer>>());
		}

		// Is each location mentioned in the text a location of the event?
		// Sometimes, they are just actors ("Ethiopia begins a massive offensive
		// in Eritrea.")
		// Set<Entity> locationEntities =
		// DBpediaAllLocationsLoader.loadLocationEntities(this.languages,
		// this.allEventPagesDataSet.getWikidataIdMappings());

		Set<Set<TextualEvent>> eventGroups = new HashSet<Set<TextualEvent>>();

		System.out.println("Merge textual events and named events.");

		// === 1. Create groups of events ===
		// Two events are out in the same set, if they have the same event date
		// and their sets of related entities are non-empty and overlap.
		for (String date : this.eventsByDates.keySet()) {
			Set<TextualEvent> doneEvents = new HashSet<TextualEvent>();
			for (TextualEvent event1 : this.eventsByDates.get(date)) {
				doneEvents.add(event1);
				for (TextualEvent event2 : this.eventsByDates.get(date)) {
					if (event1 == event2 || doneEvents.contains(event2))
						continue;
					if (event1.getRelatedEntities().isEmpty() || event2.getRelatedEntities().isEmpty())
						continue;
					if (minJaccard(event1.getRelatedEntities(), event2.getRelatedEntities()) >= JACCARD_THRESHOLD) {
						event1.addCandidateSimilarEvent(event2);
						event2.addCandidateSimilarEvent(event1);
					}
				}
			}
		}

		for (TextualEvent event : this.textualEvents) {
			Set<TextualEvent> cluster = new HashSet<TextualEvent>();
			extendEvent(event, cluster);
			eventGroups.add(cluster);
		}

		// === 2. Merge the events in each event group ===

		// three options:
		// cluster is completely new
		// cluster is new sub event
		// cluster is same as named event (=> just a new description)

		int eventNumber = 0;
		// map textual events to events
		for (Set<TextualEvent> eventGroup : eventGroups) {

			// TODO: Extra case for story (WCE) / mainEvent
			// TODO: Count by language

			Set<Event> relatedEvents = new HashSet<Event>();
			Map<Language, Map<Entity, Integer>> relatedEntities = new HashMap<Language, Map<Entity, Integer>>();
			// Map<Entity, DataSet> relatedLocations = new HashMap<Entity,
			// DataSet>();

			Map<DataSet, Set<String>> urls = new HashMap<DataSet, Set<String>>();

			// keep track of the events' datasets; to finally take the most
			// frequent one.
			Map<DataSet, Integer> dataSetsWithCount = new HashMap<DataSet, Integer>();

			Date start = null;
			Date end = null;
			for (TextualEvent event : eventGroup) {

				DataSet dataSet = DataSets.getInstance().getDataSet(event.getLanguage(), event.getSource());
				if (!dataSetsWithCount.containsKey(dataSet))
					dataSetsWithCount.put(dataSet, 1);
				else
					dataSetsWithCount.put(dataSet, dataSetsWithCount.get(dataSet) + 1);

				if (!urls.containsKey(dataSet))
					urls.put(dataSet, new HashSet<String>());
				urls.get(dataSet).add(event.getWikipediaPage());

				for (Entity relatedEntity : event.getRelatedEntities()) {
					if (!relatedEntities.containsKey(event.getLanguage()))
						relatedEntities.put(event.getLanguage(), new HashMap<Entity, Integer>());

					if (!relatedEntities.get(event.getLanguage()).containsKey(relatedEntity))
						relatedEntities.get(event.getLanguage()).put(relatedEntity, 1);
					else
						relatedEntities.get(event.getLanguage()).put(relatedEntity,
								relatedEntities.get(event.getLanguage()).get(relatedEntity) + 1);

					// if (locationEntities.contains(relatedEntity))
					// relatedLocations.put(relatedEntity, dataSet);

				}
				relatedEvents.addAll(event.getRelatedEvents());
				// if (relatedEvents.size() > 1)
				// break;
				if (start == null) {
					try {
						start = dateFormat.parse(event.getStartDate());
						end = dateFormat.parse(event.getEndDate());
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}

			// find most frequent dataset
			int maxCnt = 0;
			DataSet dataSet = null;
			for (DataSet dataSetCandidate : dataSetsWithCount.keySet()) {
				if (dataSetsWithCount.get(dataSetCandidate) > maxCnt) {
					maxCnt = dataSetsWithCount.get(dataSetCandidate);
					dataSet = dataSetCandidate;
				}
			}

			Event namedEvent = null;

			// if just one event is related and that has exactly the same time,
			// it is not a new event, just a description
			if (relatedEvents.size() == 1) {
				for (Event relatedEvent : relatedEvents)
					namedEvent = relatedEvent;
			}

			// --- create a new event for the event group or take a
			// representative named event
			Event event = null;

			if (namedEvent != null && start.equals(namedEvent.getStartTime()) && end.equals(namedEvent.getEndTime())) {
				event = namedEvent;
			} else {
				event = new Event();
				eventNumber += 1;
				event.setTemporaryId("T" + String.valueOf(eventNumber));

				DataStore.getInstance().addEvent(event);

				if (namedEvent == null) {
					// completely new event
				} else {
					event.addParent(namedEvent, dataSet);
				}
			}

			// descriptions
			for (TextualEvent eventInCluster : eventGroup) {
				Description description = new Description(event,
						DataSets.getInstance().getDataSet(eventInCluster.getLanguage(), eventInCluster.getSource()),
						eventInCluster.getText(), eventInCluster.getLanguage());
				DataStore.getInstance().addDescription(description);
			}

			// URLs
			event.setOtherURLs(urls);

			// add links
			for (Language language : relatedEntities.keySet()) {
				Map<Entity, Map<Entity, Integer>> mentionPairsInLanguage = DataStore.getInstance()
						.getMentionCountsFromTextualEvents().get(language);

				for (Entity entity1 : relatedEntities.get(language).keySet()) {
					LinksToCount linkCount = new LinksToCount(event, entity1,
							relatedEntities.get(language).get(entity1), language, true);

					DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());

					if (PUT_ENTITIES_LINKED_IN_THE_SAME_EVENT_IN_LINK_SET) {
						for (Entity entity2 : relatedEntities.get(language).keySet()) {

							if (entity1 == entity2)
								continue;

							// remember mentions of two entities in the same
							// events,
							// so these numbers can be added later to the link
							// set
							// counts
							if (!mentionPairsInLanguage.containsKey(entity1)) {
								mentionPairsInLanguage.put(entity1, new HashMap<Entity, Integer>());
							}
							if (!mentionPairsInLanguage.get(entity1).containsKey(entity2))
								mentionPairsInLanguage.get(entity1).put(entity2, 1);
							else
								mentionPairsInLanguage.get(entity1).put(entity2,
										mentionPairsInLanguage.get(entity1).get(entity2) + 1);

							if (!mentionPairsInLanguage.containsKey(entity2)) {
								mentionPairsInLanguage.put(entity2, new HashMap<Entity, Integer>());
							}
							if (!mentionPairsInLanguage.get(entity2).containsKey(entity1))
								mentionPairsInLanguage.get(entity2).put(entity1, 1);
							else
								mentionPairsInLanguage.get(entity2).put(entity1,
										mentionPairsInLanguage.get(entity2).get(entity1) + 1);
						}
					}
				}
			}

			// add locations
			// for (Entity entity : relatedLocations.keySet()) {
			// DataStore.getInstance().addLocation(new Location(event,
			// relatedLocations.get(entity), entity, null));
			// }

			// --- collect start and end times (they are the same for each
			// dataset of the events in the group)

			Set<DataSet> dataSets = new HashSet<DataSet>();
			for (TextualEvent eventInCluster : eventGroup) {
				dataSets.add(
						DataSets.getInstance().getDataSet(eventInCluster.getLanguage(), eventInCluster.getSource()));
			}

			for (DataSet dataSetWithTime : dataSets) {
				if (start != null)
					DataStore.getInstance().addStartTime(
							new StartTime(event, dataSetWithTime, new DateWithGranularity(start, DateGranularity.DAY)));
				if (end != null)
					DataStore.getInstance().addEndTime(
							new EndTime(event, dataSetWithTime, new DateWithGranularity(end, DateGranularity.DAY)));
			}

		}

	}

	private void extendEvent(TextualEvent event, Set<TextualEvent> cluster) {
		cluster.add(event);

		for (TextualEvent neighbour : event.getCandidateSimilarEvents()) {
			if (!cluster.contains(neighbour))
				extendEvent(neighbour, cluster);
		}
	}

	// private void mergeEvents() {
	//
	// Set<Set<TextualEvent>> mergedEvents = new HashSet<Set<TextualEvent>>();
	//
	// System.out.println("mergeEvents");
	//
	// for (String date : this.eventsByDates.keySet()) {
	// Set<TextualEvent> doneEvents = new HashSet<TextualEvent>();
	// for (TextualEvent event1 : this.eventsByDates.get(date)) {
	// doneEvents.add(event1);
	// for (TextualEvent event2 : this.eventsByDates.get(date)) {
	// if (event1 == event2 || doneEvents.contains(event2))
	// continue;
	// if (event1.getRelatedEntities().isEmpty() ||
	// event2.getRelatedEntities().isEmpty())
	// continue;
	// if (!Sets.intersection(event1.getRelatedEntities(),
	// event2.getRelatedEntities()).isEmpty()) {
	// event1.addCandidateSimilarEvent(event2);
	// event2.addCandidateSimilarEvent(event1);
	// }
	// }
	// }
	// }
	//
	// Set<TextualEvent> unclearEvents = new HashSet<TextualEvent>();
	// unclearEvents.addAll(this.textualEvents);
	//
	// for (TextualEvent event : this.textualEvents) {
	// if (!unclearEvents.contains(event))
	// continue;
	// if (event.getCandidateSimilarEvents().isEmpty()) {
	// unclearEvents.remove(event);
	// continue;
	// }
	// // System.out.println(event.getStartDate()+" -
	// // "+event.getEndDate());
	// // System.out.println(event.getText());
	// // for(TextualEvent event2: event.getCandidateSimilarEvents())
	// // System.out.println("\t"+event2.getText());
	// boolean clear = true;
	// for (TextualEvent event2 : event.getCandidateSimilarEvents()) {
	// // intersection should be full set -1 with is event/event2
	// if (Sets.intersection(event2.getCandidateSimilarEvents(),
	// event.getCandidateSimilarEvents())
	// .size() < event.getCandidateSimilarEvents().size() - 1) {
	// clear = false;
	// break;
	// }
	// }
	// if (clear) {
	// unclearEvents.remove(event);
	// for (TextualEvent event2 : event.getCandidateSimilarEvents()) {
	// unclearEvents.remove(event2);
	// }
	// }
	// }
	//
	// System.out.println("Unclear events: " + unclearEvents.size());
	// for (TextualEvent event : unclearEvents) {
	// Set<TextualEvent> candidates = new HashSet<TextualEvent>();
	//
	// candidates.addAll(event.getCandidateSimilarEvents());
	// for (TextualEvent event2 : event.getCandidateSimilarEvents()) {
	// candidates.addAll(event2.getCandidateSimilarEvents());
	// }
	//
	// System.out.println(candidates.size());
	// }
	//
	// }

	private double minJaccard(Set<?> set1, Set<?> set2) {
		return Sets.intersection(set1, set2).size() / Math.min(set1.size(), set2.size());
	}

	private void processFile(File file, Language language) {

		System.out.println("Process file " + file.getName() + ".");

		try {
			String content = FileLoader.readFile(file);

			if (content.isEmpty())
				return;

			for (String line : content.split(Config.NL)) {

				String[] parts = line.split(Config.TAB);
				// if(parts.length<4)

				// TODO: Remove this exception fetch. Was just used when there
				// was an error in the dumper that has been solved now.
				String wikipediaPage = null;
				try {
					wikipediaPage = "https://" + language.getLanguageLowerCase() + ".wikipedia.org/wiki/"
							+ parts[1].replaceAll(" ", "_");
				} catch (ArrayIndexOutOfBoundsException e) {
					continue;
				}

				String startDate = parts[5];
				String endDate = parts[6];
				String text = parts[7];

				if (text.trim().equals("No events.") || text.trim().equals("No events")
						|| text.trim().equals("no events") || text.trim().equals("no events."))
					continue;

				Set<Entity> relatedEntities = new HashSet<Entity>();
				Set<Event> relatedEvents = new HashSet<Event>();

				if (parts.length > 10) {
					List<String> linksAndLeadingLink = new ArrayList<String>();

					for (String entityName : parts[10].split(" ")) {
						linksAndLeadingLink.add(entityName);
					}
					if (parts.length > 11 && !parts[11].equals("null")) {
						linksAndLeadingLink.add(parts[11]);
					}

					for (String entityName : linksAndLeadingLink) {
						Entity entity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
								entityName);

						if (entity == null)
							continue;
						relatedEntities.add(entity);
						if (entity.isEvent()) {
							relatedEvents.add((Event) entity);
						}
					}
				}

				TextualEvent textualEvent = new TextualEvent(language, Source.WIKIPEDIA, null, text, relatedEntities,
						startDate, endDate, wikipediaPage);
				this.textualEvents.add(textualEvent);

				numberOfExtractedWikiEvents += 1;

				if (!this.eventsByDates.containsKey(startDate + endDate))
					this.eventsByDates.put(startDate + endDate, new HashSet<TextualEvent>());
				this.eventsByDates.get(startDate + endDate).add(textualEvent);
				// if (!this.eventsByDates.containsKey(endDate))
				// this.eventsByDates.put(endDate, new HashSet<TextualEvent>());
				// this.eventsByDates.get(endDate).add(textualEvent);

				for (Event event : relatedEvents) {
					if (!this.eventsToTextualEvents.containsKey(event))
						this.eventsToTextualEvents.put(event, new HashSet<TextualEvent>());
					this.eventsToTextualEvents.get(event).add(textualEvent);
					textualEvent.addRelatedEvent(event);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(" -> " + this.textualEvents.size() + " events.");

	}

	private int loadWCEEvents() {

		int numberOfWCEEvents = 0;

		Language language = Language.EN;

		EventsFromFileExtractor extractor = new EventsFromFileExtractor(this.getLanguages());
		extractor.loadFiles();

		for (WCEEvent wceEvent : extractor.getDataStore().getEvents()) {

			numberOfWCEEvents += 1;

			String startDate = dateFormat.format(wceEvent.getDate());
			String endDate = dateFormat.format(wceEvent.getDate());
			String text = wceEvent.getDescription();

			Set<Entity> relatedEntities = new HashSet<Entity>();
			Set<Event> relatedEvents = new HashSet<Event>();

			for (WCEEntity wceEntity : wceEvent.getEntities()) {
				Entity entity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
						wceEntity.getWikiURL().replaceAll(" ", "_"));
				if (entity == null)
					continue;
				relatedEntities.add(entity);
				if (entity.isEvent())
					relatedEvents.add((Event) entity);
			}

			Entity storyEntity = null;
			if (wceEvent.getStory() != null) {
				// add the story as related entity/event
				storyEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
						wceEvent.getStory().getName().replaceAll(" ", "_"));
				if (storyEntity != null) {
					relatedEntities.add(storyEntity);
					if (storyEntity.isEvent())
						relatedEvents.add((Event) storyEntity);
				}
			}

			// TODO: Load that information before
			String wikipediaPage = "https://en.wikipedia.org/wiki/Portal:Current_events/";
			wikipediaPage += dateFormatPageTitle.format(wceEvent.getDate());

			TextualEvent textualEvent = new TextualEvent(language, Source.WCE, null, text, relatedEntities, startDate,
					endDate, wikipediaPage);
			this.textualEvents.add(textualEvent);

			if (!this.eventsByDates.containsKey(startDate + endDate))
				this.eventsByDates.put(startDate + endDate, new HashSet<TextualEvent>());
			this.eventsByDates.get(startDate + endDate).add(textualEvent);

			if (storyEntity != null && storyEntity.isEvent())
				textualEvent.setMainEvent((Event) storyEntity);

			for (Event event : relatedEvents) {
				if (!this.eventsToTextualEvents.containsKey(event))
					this.eventsToTextualEvents.put(event, new HashSet<TextualEvent>());
				this.eventsToTextualEvents.get(event).add(textualEvent);
				textualEvent.addRelatedEvent(event);
			}
		}

		return numberOfWCEEvents;
	}

}
