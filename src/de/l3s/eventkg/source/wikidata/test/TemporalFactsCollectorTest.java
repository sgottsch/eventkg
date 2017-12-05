package de.l3s.eventkg.source.wikidata.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikidata.WikidataLoader;
import de.l3s.eventkg.source.wikidata.processors.FactsWithTemporalSnaksProcessor;

public class TemporalFactsCollectorTest extends Extractor {

	public TemporalFactsCollectorTest(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("WikidataExtractionWithEventPages", Source.WIKIDATA, "Extract relations to and from event pages.",
				languages);
	}

	public static void main(String[] args) throws IOException {

		Config.init("config_eventkb_local.txt");
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

//		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
//		allEventPagesDataSet.init();

		TemporalFactsCollectorTest temporalFactsCollectorTest = new TemporalFactsCollectorTest(languages,
				null);

		temporalFactsCollectorTest.run();
	}

	public void run() {
		FactsWithTemporalSnaksProcessor relationsToEventPagesProcessor = null;
		try {
			relationsToEventPagesProcessor = new FactsWithTemporalSnaksProcessor();
			Set<EntityDocumentDumpProcessor> processors = new HashSet<EntityDocumentDumpProcessor>();
			processors.add(relationsToEventPagesProcessor);

			WikidataLoader loader = new WikidataLoader();
			loader.loadWikidataDumpFromFile(processors);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
