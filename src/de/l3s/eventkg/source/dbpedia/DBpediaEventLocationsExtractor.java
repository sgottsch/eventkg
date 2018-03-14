package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaEventLocationsExtractor extends Extractor {

	private PrintWriter resultsWriter;

	public DBpediaEventLocationsExtractor(List<Language> languages) {
		super("DBpediaEventLocationsExtractor", Source.DBPEDIA,
				"Loads all DBpedia relations where a subject is connected with a location via <http://dbpedia.org/ontology/place>.",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> targetProperties = loadLocationProperties();
		// Set<String> targetEvents = loadTargetEvents();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_EVENT_LOCATIONS, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");
				String property = parts[1];

				// if (!targetEvents.contains(object) ||
				// !targetEvents.contains(subject))
				// continue;

				if (targetProperties.contains(property)) {

					String object = parts[2];
					String subject = parts[0];

					if (!subject.contains("resource"))
						continue;

					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					object = object.substring(object.lastIndexOf("/") + 1, object.lastIndexOf(">"));

					String fileLine = subject + Config.TAB + property + Config.TAB + object;
					if (foundEvents.contains(fileLine))
						continue;

					resultsWriter.write(fileLine + Config.NL);
					foundEvents.add(fileLine);
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

	public static Set<String> loadLocationProperties() {

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("<http://dbpedia.org/ontology/place>");

		return targetProperties;
	}

}
