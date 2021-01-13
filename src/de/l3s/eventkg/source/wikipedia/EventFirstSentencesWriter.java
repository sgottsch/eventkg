package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventFirstSentencesWriter extends Extractor {

	private WikidataIdMappings wikidataIdMappings;
	private TriplesWriter dataStoreWriter;

	private static final int MINIMUM_NUMBER_OF_CHARACTERS_IN_FIRST_SENTENCES = 20;

	public EventFirstSentencesWriter(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("EventFirstSentencesWriter", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect first sentences of events as additional descriptions.", languages);
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	public void run() {
		writeFirstSentences();
	}

	private void processFileIterator(File file, Language language) {

		System.out.println("Process file " + file.getName() + ".");

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				processLine(line, language);
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

	private void processLine(String line, Language language) {
		String[] parts = line.split(Config.TAB);
		String pageTitle = parts[1].replaceAll(" ", "_");
		Entity pageEntity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, pageTitle);

		String text = parts[2];
		text = text.replace("  ", " ");
		if (text.length() < MINIMUM_NUMBER_OF_CHARACTERS_IN_FIRST_SENTENCES)
			return;

		if (pageEntity != null && pageEntity.isEvent() && !text.isEmpty()) {
			this.dataStoreWriter.startInstance();
			this.dataStoreWriter.writeEventFirstSentence((Event) pageEntity, language, text,
					DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA));
			this.dataStoreWriter.endInstance();
		}
	}

	public void writeFirstSentences() {
		for (Language language : this.languages) {
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_FIRST_SENTENCES, language)) {
				processFileIterator(child, language);
			}
		}
	}

}
