package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaTimesExtractor extends Extractor {

	private PrintWriter resultsWriter;

	public DBpediaTimesExtractor(List<Language> languages) {
		super("DBpediaTimesExtractor", Source.DBPEDIA,
				"Loads all DBpedia relations where a subject is connected with a date via <http://dbpedia.org/ontology/date>, <http://dbpedia.org/ontology/birthDate> or <http://dbpedia.org/ontology/deathDate>.",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Map<String, TimeSymbol> targetProperties = loadTimeProperties();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_TIMES, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {

			resultsWriter.write("subject" + Config.TAB + "time" + Config.TAB + "start/end/both/no" + Config.NL);
			if (FileLoader.fileExists(FileName.DBPEDIA_MAPPINGS_LITERALS, language)) {

				try {
					br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS_LITERALS, language);
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

					if (targetProperties.containsKey(property)) {

						String timeString = parts[2];
						String subject = parts[0];
						if (!subject.contains("resource"))
							continue;

						// Date date =
						// TimeTransformer.generateEarliestTimeFromXsd(timeString);

						subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));

						String fileLine = subject + Config.TAB + property + Config.TAB + timeString + Config.TAB
								+ targetProperties.get(property).getTimeSymbol();

						if (foundEvents.contains(fileLine))
							continue;
						resultsWriter.write(fileLine + "\n");
						foundEvents.add(fileLine);
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static Map<String, TimeSymbol> loadTimeProperties() {

		// Extract this from a meta file

		Map<String, TimeSymbol> targetProperties = new HashMap<String, TimeSymbol>();

		targetProperties.put("<http://dbpedia.org/ontology/date>", TimeSymbol.START_AND_END_TIME);
		targetProperties.put("<http://dbpedia.org/ontology/birthDate>", TimeSymbol.START_TIME);
		targetProperties.put("<http://dbpedia.org/ontology/deathDate>", TimeSymbol.END_TIME);
		targetProperties.put("<http://dbpedia.org/ontology/endDate>", TimeSymbol.END_TIME);
		targetProperties.put("<http://dbpedia.org/ontology/startDate>", TimeSymbol.START_TIME);
		// targetProperties.put("<http://dbpedia.org/ontology/time>",
		// TimeSymbol.START_AND_END_TIME);

		return targetProperties;
	}

}
