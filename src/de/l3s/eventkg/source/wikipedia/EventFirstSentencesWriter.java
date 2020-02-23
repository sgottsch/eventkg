package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriter;
import de.l3s.eventkg.integration.DataStoreWriterMode;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventFirstSentencesWriter extends Extractor {

	private int lineNo;

	private AllEventPagesDataSet allEventPagesDataSet;

	public EventFirstSentencesWriter(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("LabelsAndDescriptionsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect labels and descriptions of entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		writeFirstSentences();
	}

	private void processFileIterator(File file, Language language, DataStoreWriter outputWriter, PrintWriter writer,
			PrintWriter writerPreview) {

		System.out.println("Process file " + file.getName() + ".");

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				processLine(line, language, outputWriter, writer, writerPreview);
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

	}

	private void processLine(String line, Language language, DataStoreWriter outputWriter, PrintWriter writer,
			PrintWriter writerPreview) {
		String[] parts = line.split(Config.TAB);
		String pageTitle = parts[1].replaceAll(" ", "_");
		Entity pageEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, pageTitle);

		if (pageEntity != null && pageEntity.isEvent() && !parts[2].isEmpty()) {
			outputWriter.writeFirstSentence((Event) pageEntity, language, parts[2], lineNo,
					DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA), writer, writerPreview);
			lineNo += 1;
		}
	}

	public void writeFirstSentences() {

		this.lineNo = 0;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;

		try {
			writer = FileLoader.getWriterWithAppend(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
			writerPreview = FileLoader.getWriterWithAppend(FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIEW);

			DataStoreWriter outputWriter = new DataStoreWriter(languages, allEventPagesDataSet,
					DataStoreWriterMode.USE_IDS_OF_CURRENT_EVENTKG_VERSION);
			outputWriter.init();

			for (Language language : this.languages) {
				for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_FIRST_SENTENCES, language)) {
					processFileIterator(child, language, outputWriter, writer, writerPreview);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

}
