package de.l3s.eventkg.source.wikidata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikidata.processors.RelationsToEventPagesProcessor;

public class WikidataExtractionWithEventPages extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public WikidataExtractionWithEventPages(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("WikidataExtractionWithEventPages", Source.WIKIDATA,
				"Loads all Wikidata relations where the subject and/or object is an event or both have an existence time.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {

		RelationsToEventPagesProcessor relationsToEventPagesProcessor = null;
		relationsToEventPagesProcessor = new RelationsToEventPagesProcessor(allEventPagesDataSet);

		Set<EntityDocumentDumpProcessor> processors = new HashSet<EntityDocumentDumpProcessor>();
		processors.add(relationsToEventPagesProcessor);

		WikidataLoader loader = new WikidataLoader();
		loader.loadWikidataDumpFromFile(processors);
	}

}
