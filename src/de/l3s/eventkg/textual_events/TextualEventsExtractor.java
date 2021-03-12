package de.l3s.eventkg.textual_events;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.EntityIdGenerator;
import de.l3s.eventkg.integration.TextualEvent;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.collection.EventDependency;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.source.currentevents.EventsFromFileExtractor;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;
import de.l3s.eventkg.source.wikipedia.model.LinksToCountNew;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class TextualEventsExtractor extends Extractor {

	private int numberOfExtractedWikiEvents = 0;

	private static final int MINIMUM_NUMBER_OF_CHARACTERS_IN_WIKIPEDIA_EVENTS = 20;

	private Set<TextualEvent> textualEvents;
	private Map<Event, Set<TextualEvent>> eventsToTextualEvents;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);
	private SimpleDateFormat dateFormatPageTitle = new SimpleDateFormat("MMMMM_yyyy", Locale.US);

	private Map<String, Set<TextualEvent>> eventsByDates = new THashMap<String, Set<TextualEvent>>();

	private double JACCARD_THRESHOLD = 0.2;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter triplesWriter;

	private Map<Language, PrintWriter> linkWriters;

	private PrefixList prefixList;

	private int eventNo;

	private EntityIdGenerator idGeneratorPreviousVersion;

	private Map<Event, Date> namedEventStartTimes;
	private Map<Event, Date> namedEventEndTimes;

	public TextualEventsExtractor(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("TextualEventsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect and integrate textual events and their relations.", languages);
		this.languages = languages;
		this.wikidataIdMappings = wikidataIdMappings;
		this.triplesWriter = dataStoreWriter;
	}

	public void run() {
		prefixList = PrefixList.getInstance();
		try {
			init();
			extractRelations();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			for (PrintWriter writer : this.linkWriters.values())
				writer.close();
		}
	}

	private void init() throws FileNotFoundException {
		initLinksWriters();

		loadNamedEventDates();

		// we can only link textual events to those from previous version. But
		// for entities, we use the current version. Therefore, we use two ID
		// generators (one here, one in the writer).

		this.idGeneratorPreviousVersion = new EntityIdGenerator(true);
		
		EntityIdGenerator idGeneratorCurrentVersion = new EntityIdGenerator(false);
		this.eventNo = idGeneratorCurrentVersion.getLastEventNo() + 1;
	}

	private void loadNamedEventDates() {

		System.out.println("load named event dates");

		this.namedEventStartTimes = new HashMap<Event, Date>();
		this.namedEventEndTimes = new HashMap<Event, Date>();

		SimpleDateFormat dateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.TIMES_INTEGRATED);
			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split(",");
				String eventWikidataId = parts[0];
				Event event = this.wikidataIdMappings.getEventByWikidataId(eventWikidataId);
				String dateString = parts[2];

				try {
					Date date = dateFormat.parse(dateString);

					if (parts[1].equals("B"))
						this.namedEventStartTimes.put(event, date);
					else
						this.namedEventEndTimes.put(event, date);
				} catch (ParseException e) {
					e.printStackTrace();
				}

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

		System.out.println("#Events with start times: " + this.namedEventStartTimes.keySet().size());
		System.out.println("#Events with end times: " + this.namedEventEndTimes.keySet().size());
	}

	private void extractRelations() {

		this.textualEvents = new THashSet<TextualEvent>();
		this.eventsToTextualEvents = new THashMap<Event, Set<TextualEvent>>();

		loadTextualEventsFromWikipedia();
		loadTextualEventsFromWCE();
		System.out.println("Total number of textual events: " + this.textualEvents.size() + ".");

		mergeEvents();
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
			DateGranularity startGranularity = null;
			Date end = null;
			DateGranularity endGranularity = null;
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
						startGranularity = event.getGranularity();
						end = dateFormat.parse(event.getEndDate());
						endGranularity = event.getGranularity();
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
			if (namedEvent != null) {
				if (this.namedEventStartTimes.containsKey(namedEvent)
						&& start.equals(namedEventStartTimes.get(namedEvent))
						&& this.namedEventEndTimes.containsKey(namedEvent)
						&& end.equals(namedEventEndTimes.get(namedEvent))) {
					writeNamedEventDescriptions(namedEvent, eventGroup);
					continue;
				}
			}

			Event event = new Event();
			// String eventId = "text_event_" + String.valueOf(eventNumber);
			// event.setId(eventId);
			// eventNumber += 1;

			if (namedEvent != null)
				event.addParent(namedEvent, dataSet);

			// descriptions and categories
			for (TextualEvent eventInCluster : eventGroup) {

				event.addDescription(
						DataSets.getInstance().getDataSet(eventInCluster.getLanguage(), eventInCluster.getSource()),
						eventInCluster.getLanguage(), eventInCluster.getText());

				// DataStore.getInstance().addDescription(description);
				if (eventInCluster.getEnglishWCECategory() != null) {
					// categories from WCE (English only)
					event.addCategory(DataSets.getInstance().getDataSet(Language.EN, Source.WCE), Language.EN,
							eventInCluster.getEnglishWCECategory());
				}
				for (Language language : eventInCluster.getOtherCategories().keySet()) {
					for (String otherCategory : eventInCluster.getOtherCategories().get(language)) {
						// categories from date page section titles
						event.addCategory(DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA), language,
								otherCategory);
					}
				}
				for (String source : eventInCluster.getSources())
					event.addSource(DataSets.getInstance().getDataSet(Language.EN, Source.WCE), source);
			}

			// URLs
			event.setOtherURLs(urls);

			// add links
			for (Language language : relatedEntities.keySet()) {
				writeLinkLine(language, relatedEntities.get(language).keySet());
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
					event.addStartTime(new DateWithGranularity(start, startGranularity), dataSetWithTime);
				if (end != null)
					event.addEndTime(new DateWithGranularity(end, endGranularity), dataSetWithTime);
			}

			Set<Entity> relatedEntitiesUnique = new HashSet<Entity>();
			for (Language language : relatedEntities.keySet()) {
				relatedEntitiesUnique.addAll(relatedEntities.get(language).keySet());
			}

			writeTextualEvent(event, relatedEntitiesUnique);
			writeEventLinkTriples(event, relatedEntities);
		}

	}

	private void extendEvent(TextualEvent event, Set<TextualEvent> cluster) {
		cluster.add(event);

		for (TextualEvent neighbour : event.getCandidateSimilarEvents()) {
			if (!cluster.contains(neighbour))
				extendEvent(neighbour, cluster);
		}
	}

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

				DateGranularity granularity = DateGranularity.valueOf(parts[9]);

				if (text.trim().equals("No events.") || text.trim().equals("No events")
						|| text.trim().equals("no events") || text.trim().equals("no events."))
					continue;

				text = text.replace("  ", " ");
				if (text.length() < MINIMUM_NUMBER_OF_CHARACTERS_IN_WIKIPEDIA_EVENTS)
					continue;

				Set<Entity> relatedEntities = new HashSet<Entity>();
				Set<Event> relatedEvents = new HashSet<Event>();

				Map<Language, Set<String>> categories = new HashMap<Language, Set<String>>();
				categories.put(language, new HashSet<String>());

				if (parts.length > 10) {
					List<String> linksAndLeadingLink = new ArrayList<String>();

					for (String entityName : parts[10].split(" ")) {
						linksAndLeadingLink.add(entityName);
					}
					if (parts.length > 11 && !parts[11].equals("null")) {
						linksAndLeadingLink.add(parts[11]);
					}

					if (parts.length > 12 && !parts[12].equals("null")) {
						for (String category : parts[12].split(";"))
							categories.get(language).add(category);
					}

					for (String entityName : linksAndLeadingLink) {
						Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, entityName);

						if (entity == null)
							continue;
						relatedEntities.add(entity);
						if (entity.isEvent()) {
							relatedEvents.add((Event) entity);
						}
					}
				}

				TextualEvent textualEvent = new TextualEvent(language, Source.WIKIPEDIA, null, text, relatedEntities,
						startDate, endDate, wikipediaPage, granularity);

				textualEvent.setOtherCategories(categories);

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
			text = text.replace("  ", " ");

			Set<Entity> relatedEntities = new HashSet<Entity>();
			Set<Event> relatedEvents = new HashSet<Event>();
			Set<String> sources = new HashSet<String>();

			for (WCEEntity wceEntity : wceEvent.getEntities()) {
				Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language,
						wceEntity.getWikiURL().replaceAll(" ", "_"));
				if (entity == null)
					continue;
				relatedEntities.add(entity);
				if (entity.isEvent())
					relatedEvents.add((Event) entity);
			}

			for (de.l3s.eventkg.source.currentevents.model.Source source : wceEvent.getSources()) {
				sources.add(source.getUrl());
			}

			Entity storyEntity = null;
			if (wceEvent.getStory() != null) {
				// add the story as related entity/event
				storyEntity = this.wikidataIdMappings.getEntityByWikipediaLabel(language,
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
					endDate, wikipediaPage, DateGranularity.DAY);
			textualEvent.setSources(sources);

			this.textualEvents.add(textualEvent);
			if (wceEvent.getCategory() != null)
				textualEvent.setEnglishWCECategory(wceEvent.getCategory().getName());

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

	private void initLinksWriters() throws FileNotFoundException {
		this.linkWriters = new HashMap<Language, PrintWriter>();
		for (Language language : this.languages) {
			PrintWriter linksWriter = FileLoader.getWriter(FileName.TEXT_EVENT_LINKS, language);
			this.linkWriters.put(language, linksWriter);
		}
	}

	private void writeLinkLine(Language language, Set<Entity> entities) {
		Set<String> entityNames = new HashSet<String>();
		for (Entity entity : entities) {
			entityNames.add(entity.getWikidataId());
		}
		linkWriters.get(language).println(StringUtils.join(entityNames, ","));
	}

	private void writeNamedEventDescriptions(Event namedEvent, Set<TextualEvent> eventGroup) {
		this.triplesWriter.startInstance();
		for (TextualEvent textEvent : eventGroup) {
			this.triplesWriter.writeEventTextualEventDescription(namedEvent, textEvent.getLanguage(),
					textEvent.getText(),
					DataSets.getInstance().getDataSet(textEvent.getLanguage(), textEvent.getSource()));
		}
		this.triplesWriter.endInstance();
	}

	private void writeTextualEvent(Event event, Set<Entity> relatedEntities) {

		triplesWriter.startInstance();

		event.setTextEvent(true);

		String eventId = this.idGeneratorPreviousVersion.getEventID(event);
		if (eventId == null) {
			eventId = "event_" + String.valueOf(eventNo);
			eventNo += 1;
		}
		event.setId(eventId);

		// type
		triplesWriter.writeBasicTypeTriple(event, this.prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "TextEvent",
				DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);

		// URLs
		for (DataSet dataSet : event.getOtherUrls().keySet()) {
			for (String otherUrl : event.getOtherUrls().get(dataSet)) {
				triplesWriter.writeBasicExtractedTriple(event, otherUrl, dataSet, true);
			}
		}

		// times
		boolean firstDate = true;
		for (DateWithGranularity startTime : event.getStartTimesWithDataSets().keySet()) {
			for (DataSet ds : event.getStartTimesWithDataSets().get(startTime)) {
				triplesWriter.writeTime(event, startTime, ds, true, firstDate);
				firstDate = false;
			}
		}

		firstDate = true;
		for (DateWithGranularity endTime : event.getEndTimesWithDataSets().keySet()) {
			for (DataSet ds : event.getEndTimesWithDataSets().get(endTime)) {
				triplesWriter.writeTime(event, endTime, ds, false, firstDate);
				firstDate = false;
			}
		}

		// descriptions
		Map<Language, Set<String>> names = new HashMap<Language, Set<String>>();
		for (Description description : event.getDescriptions()) {

			if (!names.containsKey(description.getLanguage()))
				names.put(description.getLanguage(), new HashSet<String>());

			Language language = description.getLanguage();
			triplesWriter.writeBasicDescriptionTriple(event, description.getLabel(), description.getDataSet(), language,
					!names.get(description.getLanguage()).contains(description.getLabel()));
			names.get(description.getLanguage()).add(description.getLabel());
		}

		// parent events
		for (Event parentEvent : event.getParents()) {
			triplesWriter.writeEventDependency(parentEvent, event, EventDependency.SUB_EVENT, true);
			triplesWriter.writeEventDependency(event, parentEvent, EventDependency.SUB_EVENT_OF, true);
		}

		// categories
		for (DataSet categoryDataSet : event.getCategories().keySet()) {
			for (Language categoryLanguage : event.getCategories().get(categoryDataSet).keySet()) {
				for (String category : event.getCategories().get(categoryDataSet).get(categoryLanguage)) {
					triplesWriter.writeTextEventCategoryTriple(event, category, categoryLanguage, categoryDataSet,
							true);
				}
			}
		}

		// sources
		for (DataSet sourceDataSet : event.getSources().keySet()) {
			for (String source : event.getSources().get(sourceDataSet)) {
				triplesWriter.writeTextEventSourceTriple(event, source, sourceDataSet, true);
			}
		}

		for (Entity entity : relatedEntities) {
			triplesWriter.writeTextEventActorTriple(event, entity);
		}

		triplesWriter.endInstance();
	}

	private void writeEventLinkTriples(Event event, Map<Language, Map<Entity, Integer>> relatedEntities) {

		triplesWriter.startInstance();

		Map<Entity, LinksToCountNew> linkCounts = new HashMap<Entity, LinksToCountNew>();

		for (Language language : relatedEntities.keySet()) {
			for (Entity entity : relatedEntities.get(language).keySet()) {

				LinksToCountNew linkCount = linkCounts.get(entity);

				if (linkCount == null) {
					linkCount = new LinksToCountNew(event, entity, true);
					linkCounts.put(entity, linkCount);
				}

				int count = relatedEntities.get(language).get(entity);

				linkCount.addCount(DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA), count);
			}
		}

		for (LinksToCountNew linkCount : linkCounts.values())
			triplesWriter.writeTextEventLinkCount(linkCount);

		triplesWriter.endInstance();
	}

}
