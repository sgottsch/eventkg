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
import de.l3s.eventkg.source.wikidata.processors.PositionsProcessor;

public class ProcessorTest extends Extractor {

	public ProcessorTest(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("WikidataProcessorTest", Source.WIKIDATA, "Test Wikidata processor.", languages);
	}

	public static void main(String[] args) throws IOException {

		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

		// AllEventPagesDataSet allEventPagesDataSet = new
		// AllEventPagesDataSet(languages);
		// allEventPagesDataSet.init();

		ProcessorTest processorTest = new ProcessorTest(languages, null);

		processorTest.run();
	}

	public void run() {
		PositionsProcessor processor = null;
		try {
			processor = new PositionsProcessor();
			Set<EntityDocumentDumpProcessor> processors = new HashSet<EntityDocumentDumpProcessor>();
			processors.add(processor);

			WikidataLoader loader = new WikidataLoader();
			loader.loadWikidataDumpFromFile(processors);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
