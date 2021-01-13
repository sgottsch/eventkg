package de.l3s.eventkg.integration.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.pipeline.output.RDFWriterName;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventDependenciesCollector extends Extractor {

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter dataStoreWriter;

	public EventDependenciesCollector(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("EventDependenciesCollector", Source.ALL,
				"Collect event dependencies (sub event, previous events, next events, ...).", languages);
		this.languages = languages;
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	@Override
	public void run() {
		loadEventDependencies();
		clear();
	}

	private void clear() {
		this.dataStoreWriter.resetNumberOfInstances(RDFWriterName.EVENT_BASE_RELATIONS);
		for (Entity entity : this.wikidataIdMappings.getEntities()) {
			if (entity.isEvent()) {
				((Event) entity).clearEventDependencies();
			}
		}
	}

	public void loadEventDependencies() {
		System.out.println("loadSubEvents");
		loadSubEvents();
		System.out.println("loadPreviousEvents");
		loadPreviousEvents();
		System.out.println("loadNextEvents");
		loadNextEvents();
	}

	private void loadSubEvents() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_PART_OF_RELATIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Event parentEvent = this.wikidataIdMappings.getEventByWikidataId(parts[0]);
				if (parentEvent == null)
					continue;

				Event event = this.wikidataIdMappings.getEventByWikidataId(parts[2]);
				if (event == null)
					continue;

				// String dataSetId = parts[4];

				dataStoreWriter.startInstance();
				if (!event.getParents().contains(parentEvent)) {
					event.addParent(parentEvent);
					dataStoreWriter.writeEventDependency(parentEvent, event, EventDependency.SUB_EVENT, true);
				}
				if (!parentEvent.getChildren().contains(event)) {
					parentEvent.addChild(event);
					dataStoreWriter.writeEventDependency(event, parentEvent, EventDependency.SUB_EVENT_OF, true);
				}
				dataStoreWriter.endInstance();

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

				Event event1 = this.wikidataIdMappings.getEventByWikidataId(parts[0]);
				if (event1 == null)
					continue;

				Event event2 = this.wikidataIdMappings.getEventByWikidataId(parts[2]);
				if (event2 == null)
					continue;

				// String dataSetId = parts[4];

				if (!event1.getPreviousEvents().contains(event2)) {
					dataStoreWriter.startInstance();
					event1.addPreviousEvent(event2);
					dataStoreWriter.writeEventDependency(event1, event2, EventDependency.PREVIOUS_EVENT, true);
					dataStoreWriter.endInstance();
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

	private void loadNextEvents() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_NEXT_EVENTS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Event event1 = this.wikidataIdMappings.getEventByWikidataId(parts[0]);
				if (event1 == null)
					continue;

				Event event2 = this.wikidataIdMappings.getEventByWikidataId(parts[2]);
				if (event2 == null)
					continue;

				// String dataSetId = parts[4];

				if (!event1.getNextEvents().contains(event2)) {
					dataStoreWriter.startInstance();
					event1.addNextEvent(event2);
					dataStoreWriter.writeEventDependency(event1, event2, EventDependency.NEXT_EVENT, true);
					dataStoreWriter.endInstance();
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
