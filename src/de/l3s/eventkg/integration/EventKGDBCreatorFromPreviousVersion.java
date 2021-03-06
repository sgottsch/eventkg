package de.l3s.eventkg.integration;

import java.util.List;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class EventKGDBCreatorFromPreviousVersion extends Extractor {

	public EventKGDBCreatorFromPreviousVersion(List<Language> languages) {
		super("DBpediaEventRelationsExtractor", Source.ALL,
				"Creates a database with mappings to re-use old IDs from the previous EventKG version.", languages);
	}

	@Override
	public void run() {
		EventKGDBCreator dbCreator = new EventKGDBCreator(languages, true);
		dbCreator.createWikidataMappingIDDB();
	}

}
