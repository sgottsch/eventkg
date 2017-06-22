package de.l3s.eventkg.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaTimesExtractor extends Extractor {

	private PrintWriter resultsWriter;

	public DBpediaTimesExtractor(List<Language> languages) {
		super("DBpediaTimesExtractor", Source.DBPEDIA, "?", languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> targetProperties = loadTimeProperties();
		// Set<String> targetEvents = loadTargetEvents();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_TIMES, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS_LITERALS, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			resultsWriter.write("subject" + "\t" + "time" + "\n");

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

					String timeString = parts[2];
					String subject = parts[0];
					if (!subject.contains("resource"))
						continue;

					// Date date =
					// TimeTransformer.generateEarliestTimeFromXsd(timeString);

					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));

					String fileLine = subject + Config.TAB + property + Config.TAB + timeString;

					if (foundEvents.contains(fileLine))
						continue;
					resultsWriter.write(fileLine + "\n");
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

	public static Set<String> loadTimeProperties() {

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("<http://dbpedia.org/ontology/date>");

		return targetProperties;
	}

}
