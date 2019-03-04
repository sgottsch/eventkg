package de.l3s.eventkg.source.currentevents;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class CurrentEventsRelationsExtraction extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public CurrentEventsRelationsExtraction(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("CurrentEventsRelationsExtraction", de.l3s.eventkg.meta.Source.WCE,
				"Extract relations between entities and events for the textual events from the Wikipedia Current Events Portal.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect event pages.");
		extractRelations();
	}

	private void extractRelations() {

		PrintWriter relationsWriter = null;

		System.out.println("Extract relations.");

		try {
			relationsWriter = FileLoader.getWriter(FileName.WCE_EVENT_RELATIONS);

			EventsFromFileExtractor extractor = new EventsFromFileExtractor(this.getLanguages());
			extractor.loadFiles();

			System.out.println(extractor.getDataStore().getEvents().size() + " events.");

			for (WCEEvent event : extractor.getDataStore().getEvents()) {

				Set<String> eventPages = new HashSet<String>();

				if (event.getStory() != null && allEventPagesDataSet.getEventByWikipediaLabel(Language.EN,
						event.getStory().getName().replaceAll(" ", "_")) != null) {
					eventPages.add(event.getStory().getName().replaceAll(" ", "_"));
				}

				for (WCEEntity entity : event.getEntities()) {
					if (allEventPagesDataSet.getEventByWikipediaLabel(Language.EN,
							entity.getWikiURL().replaceAll(" ", "_")) != null) {
						eventPages.add(entity.getWikiURL().replaceAll(" ", "_"));
					}
				}

				for (String eventPage : eventPages) {
					// System.out.println(eventPage);
					for (WCEEntity entity : event.getEntities()) {
						String entityName = entity.getWikiURL().replaceAll(" ", "_");
						if (!entityName.equals(eventPage))
							relationsWriter.write(entityName + "\t" + eventPage + "\t"
									+ FileLoader.PARSE_DATE_FORMAT.format(event.getDate()) + "\n");
					}
				}
			}

		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} finally {
			relationsWriter.close();
		}

	}

}
