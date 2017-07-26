package de.l3s.eventkg.dbpedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaEventRelationsExtractor extends Extractor {

	private PrintWriter resultsWriter;
	private AllEventPagesDataSet allEventPagesDataSet;

	public DBpediaEventRelationsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("DBpediaEventRelationsExtractor", Source.DBPEDIA, "?", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> forbiddenProperties = DBpediaEventLocationsExtractor.loadLocationProperties();
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadPartOfProperties());
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadNextEventProperties());
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadPreviousEventProperties());

		BufferedReader br = null;

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_EVENT_RELATIONS, language);
			br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS, language);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");
				String object = parts[2];
				String subject = parts[0];
				String property = parts[1];

				// TODO: ignore even more
				// ignore locations
				if (forbiddenProperties.contains(property)) {
					continue;
				}
				if (property.equals("rdf-schema#seeAlso") || property.equals("owl#differentFrom"))
					continue;

				if (!subject.contains("resource"))
					continue;

				try {
					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					object = object.substring(object.lastIndexOf("/") + 1, object.lastIndexOf(">"));
				} catch (StringIndexOutOfBoundsException e) {
					// skip objects like
					// "http://fr.dbpedia.org/resource/Sultanat_d'Égypte__1"@fr
					// .
					continue;
				}

				if (this.allEventPagesDataSet.getEventByWikipediaLabel(language, subject) != null
						|| this.allEventPagesDataSet.getEventByWikipediaLabel(language, object) != null) {
					resultsWriter.write(subject + Config.TAB + property + Config.TAB + object + Config.NL);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
