package de.l3s.eventkg.statistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LanguageCoverage {

	public static void main(String[] args) {

		test();

		Config.init(args[0]);

		Set<Language> languages = new HashSet<Language>();
		Map<String, Set<Language>> languagesPerEvent = new HashMap<String, Set<Language>>();

		System.out.println(FileLoader.getPath(FileName.ALL_TTL_EVENTS_WITH_TEXTS));

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				if (parts[1].equals("<http://www.w3.org/2000/01/rdf-schema#label>")
						|| parts[1].equals("<http://purl.org/dc/terms/description>")) {

					try {
						String part = line.substring(line.indexOf(" ")).trim();
						part = part.substring(part.indexOf(" ")).trim();
						String langStr = part.substring(part.lastIndexOf("@") + 1, part.lastIndexOf("@") + 3);
						Language language = Language.getLanguage(langStr.toUpperCase());

						String eventId = parts[0];
						if (!languagesPerEvent.containsKey(eventId))
							languagesPerEvent.put(eventId, new HashSet<Language>());
						languages.add(language);
						languagesPerEvent.get(eventId).add(language);
					} catch (IllegalArgumentException e) {
						System.out.println(e.getMessage() + ": " + line);
						continue;
					}
				}
			}

			for (Language language : languages) {

				System.out.println("--- " + language.getLanguage() + " ---");
				int numberOfEventsWithLanguage = 0;
				int numberOfEventsWithLanguageOnly = 0;

				for (String eventId : languagesPerEvent.keySet()) {
					Set<Language> eventLanguages = languagesPerEvent.get(eventId);
					if (eventLanguages.contains(language)) {
						numberOfEventsWithLanguage += 1;
						if (eventLanguages.size() == 1)
							numberOfEventsWithLanguageOnly += 1;
					}
				}

				System.out.println(language.getLanguage() + "\t" + numberOfEventsWithLanguage + "\t"
						+ numberOfEventsWithLanguageOnly);
			}

			System.out.println("--- all ---");
			int eventsInAllLanguages = 0;
			for (String eventId : languagesPerEvent.keySet()) {
				Set<Language> eventLanguages = languagesPerEvent.get(eventId);
				if (eventLanguages.size() == languages.size())
					eventsInAllLanguages += 1;
			}
			System.out.println("all\t" + languagesPerEvent.keySet().size() + "\t" + eventsInAllLanguages);

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

	public static void test() {

		String line = "<http://eventKG.l3s.uni-hannover.de/resource/event_1300022> <http://purl.org/dc/terms/description> \"Ugandan High @Court Justice V.F. Kibuuka Musoke rules that Rolling Stone violated the civil rights of homosexuals when it printed their pictures on the front page with the headline \\\"Hang Them.\\\" The court orders the newspaper to pay each of the three lead plaintiffs $1.5 million Ugandan shillings.\"@en <http://eventKG.l3s.uni-hannover.de/graph/wikipedia_en> .";
		String part = line.substring(line.indexOf(" ")).trim();
		part = part.substring(part.indexOf(" ")).trim();
		String langStr = part.substring(part.lastIndexOf("@") + 1, part.lastIndexOf("@") + 3);

		System.out.println(langStr);
		Language language = Language.getLanguage(langStr.toUpperCase());

	}

}
