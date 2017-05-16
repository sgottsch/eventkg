package de.l3s.eventkg.wikidata;

import java.util.HashSet;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.EntityTimerProcessor;
import org.wikidata.wdtk.dumpfiles.MwLocalDumpFile;

import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class WikidataLoader {

	public void loadWikidataDumpFromFile(EntityDocumentDumpProcessor processor) {
		Set<EntityDocumentDumpProcessor> processors = new HashSet<EntityDocumentDumpProcessor>();
		processors.add(processor);
		loadWikidataDumpFromFile(processors);
	}

	public void loadWikidataDumpFromFile(Set<EntityDocumentDumpProcessor> processors) {

		DumpProcessingController dumpProcessingController = new DumpProcessingController("wikidatawiki");

		for (EntityDocumentDumpProcessor processor : processors)
			dumpProcessingController.registerEntityDocumentProcessor(processor, null, true);

		EntityTimerProcessor entityTimerProcessor = new EntityTimerProcessor(0);
		dumpProcessingController.registerEntityDocumentProcessor(entityTimerProcessor, null, true);

		// Select local file (meta-data will be guessed):
		// System.out.println();
		// System.out.println("Processing a local dump file giving only its
		// location");
		// System.out.println("(meta-data like the date is guessed from the file
		// name):");
		// MwLocalDumpFile mwDumpFile = new MwLocalDumpFile(DUMP_FILE);
		// dumpProcessingController.processDump(mwDumpFile);

		// Select local file and set meta-data:
		System.out.println();
		System.out.println("Processing a local dump file with all meta-data set:");
		System.out.println(FileLoader.getFileNameWithPath(FileName.WIKIDATA_DUMP));
		MwLocalDumpFile mwDumpFile = new MwLocalDumpFile(FileLoader.getFileNameWithPath(FileName.WIKIDATA_DUMP),
				DumpContentType.JSON, null, "wikidatawiki");
		dumpProcessingController.processDump(mwDumpFile);

		entityTimerProcessor.close();
		for (EntityDocumentDumpProcessor processor : processors)
			processor.close();

	}

}
