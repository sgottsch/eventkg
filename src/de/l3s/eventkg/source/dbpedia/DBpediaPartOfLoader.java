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

public class DBpediaPartOfLoader extends Extractor {

	private PrintWriter resultsWriter;
	private PrintWriter resultsWriterPreviousEvents;
	private PrintWriter resultsWriterNextEvents;

	public DBpediaPartOfLoader(List<Language> languages) {
		super("DBpediaPartOfLoader", Source.DBPEDIA,
				"Loads all DBpedia relations where a subject is connected with an object as its part (e.g., via <http://dbpedia.org/ontology/isPartOf>), it's predecessor (e.g., via <http://dbpedia.org/ontology/previousEvent>), or succesor (e.g., via <http://dbpedia.org/ontology/followingEvent>).",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> targetProperties = loadPartOfProperties();
		Set<String> targetPropertiesNext = loadNextEventProperties();
		Set<String> targetPropertiesPrevious = loadPreviousEventProperties();
		// Set<String> targetEvents = loadTargetEvents();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_DBO_EVENT_PARTS, language);
			resultsWriterPreviousEvents = FileLoader.getWriter(FileName.DBPEDIA_DBO_PREVIOUS_EVENTS, language);
			resultsWriterNextEvents = FileLoader.getWriter(FileName.DBPEDIA_DBO_NEXT_EVENTS, language);
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

				if (targetProperties.contains(property) || targetPropertiesNext.contains(property)
						|| targetPropertiesPrevious.contains(property)) {

					String object = parts[2];
					String subject = parts[0];

					String fileLine = subject + Config.TAB + property + Config.TAB + object;

					if (foundEvents.contains(fileLine))
						continue;

					if (targetProperties.contains(property))
						resultsWriter.write(fileLine + "\n");
					else if (targetPropertiesNext.contains(property))
						resultsWriterNextEvents.write(fileLine + "\n");
					else if (targetPropertiesPrevious.contains(property))
						resultsWriterPreviousEvents.write(fileLine + "\n");

					foundEvents.add(fileLine);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				resultsWriter.close();
				resultsWriterPreviousEvents.close();
				resultsWriterNextEvents.close();
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

	public static Set<String> loadPartOfProperties() {

		// Search for properties that are part of partOf property. Do this with
		// SPARQL ("?subProperty rdfs:subPropertyOf* dbo:isPartOf "). Full
		// query:
		// http://dbpedia.org/snorql/?query=SELECT+%3FsubProperty+%3Fparent+WHERE+{%0D%0A%3FsubProperty+rdfs%3AsubPropertyOf*+dbo%3AisPartOf+.%0D%0A}%0D%0A

		Set<String> targetProperties = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_PART_OF_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				targetProperties.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return targetProperties;
	}

	public static Set<String> loadPreviousEventProperties() {
		Set<String> targetProperties = new HashSet<String>();
		targetProperties.add("<http://dbpedia.org/ontology/previousEvent>");
		// Eurovision Song Contest has "previousWork" instead of "previousEvent"
		targetProperties.add("<http://dbpedia.org/ontology/previousWork>");
		return targetProperties;
	}

	public static Set<String> loadNextEventProperties() {
		Set<String> targetProperties = new HashSet<String>();
		targetProperties.add("<http://dbpedia.org/ontology/followingEvent>");
		// Eurovision Song Contest has "subsequentWork" instead of
		// "followingEvent"
		targetProperties.add("<http://dbpedia.org/ontology/subsequentWork>");
		return targetProperties;
	}

}
