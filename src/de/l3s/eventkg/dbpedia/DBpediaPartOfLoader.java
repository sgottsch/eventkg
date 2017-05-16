package de.l3s.eventkg.dbpedia;

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

public class DBpediaPartOfLoader extends Extractor {

	private PrintWriter resultsWriter;

	public DBpediaPartOfLoader(List<Language> languages) {
		super("DBpediaPartOfLoader", Source.DBPEDIA, "?", languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> targetProperties = loadPartOfProperties();
		// Set<String> targetEvents = loadTargetEvents();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_DBO_EVENT_PARTS, language);
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

				if (targetProperties.contains(property)) {

					String object = parts[2];
					String subject = parts[0];

					String fileLine = subject + Config.TAB + property + Config.TAB + object;

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

	// private Set<String> loadTargetEvents() {
	//
	// Set<String> targetEvents = new HashSet<String>();
	//
	// BufferedReader br = null;
	// try {
	// try {
	// br = new BufferedReader(new InputStreamReader(
	// new
	// FileInputStream(DBpediaDBOEventsLoader.DBPEDIA_DBO_EVENTS_FILE_NAME)));
	// } catch (FileNotFoundException e1) {
	// e1.printStackTrace();
	// }
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	// targetEvents.add(line);
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// br.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// return targetEvents;
	// }

	private Set<String> loadPartOfProperties() {

		// Search for properties that are part of partOf property. Do this with
		// SPARQL ("?subProperty rdfs:subPropertyOf* dbo:isPartOf "). Full
		// query:
		// http://dbpedia.org/snorql/?query=SELECT+%3FsubProperty+%3Fparent+WHERE+{%0D%0A%3FsubProperty+rdfs%3AsubPropertyOf*+dbo%3AisPartOf+.%0D%0A}%0D%0A

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("<http://dbpedia.org/ontology/isPartOfMilitaryConflict>");
		targetProperties.add("<http://dbpedia.org/ontology/isPartOf>");
		targetProperties.add("<http://dbpedia.org/ontology/isPartOfWineRegion>");
		targetProperties.add("<http://dbpedia.org/ontology/isPartOfAnatomicalStructure>");

		return targetProperties;
	}

}
