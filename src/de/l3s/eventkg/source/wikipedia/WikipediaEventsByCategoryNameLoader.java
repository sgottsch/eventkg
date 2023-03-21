package de.l3s.eventkg.source.wikipedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class WikipediaEventsByCategoryNameLoader extends Extractor {

	public WikipediaEventsByCategoryNameLoader(List<Language> languages) {
		super("WikipediaEventsLoader", Source.WIKIPEDIA,
				"Load Wikipedia pages representing events according to their category names (e.g. English Wikipedia pages in the category \"November_1963_events\").",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public static void run(Language language) {

		Map<String, String> eventPages = new HashMap<String, String>();

		// no need to do the process if there are no event categories specified
		// but even if there is none, create the empty file (so when loading the
		// files later, we don't need to check for file existence)
		if (WikiWords.getInstance().getEventCategoryRegexes(language) != null
				&& !WikiWords.getInstance().getEventCategoryRegexes(language).isEmpty()) {
			System.out.println("Load event pages (Wikipedia categories) - " + language + ".");
			Map<Integer, String> eventPageIDs = loadEventPages(language);
			System.out.println("Load labels of Wikipedia category event pages.");
			eventPages = addLabelsToEventPages(eventPageIDs, language);
		}

		writeResults(eventPages, language);
		System.out.println("Done loading Wikipedia category event pages.");
	}

	private static void writeResults(Map<String, String> eventPages, Language language) {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.WIKIPEDIA_EVENTS, language);

			for (String page : eventPages.keySet()) {
				writer.write(page);
				writer.write(Config.TAB);
				writer.write(eventPages.get(page));
				writer.write(Config.NL);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private static Map<String, String> addLabelsToEventPages(Map<Integer, String> redirectsWithIds, Language language) {

		Map<String, String> eventPages = new HashMap<String, String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIPEDIA_PAGE_INFOS, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("INSERT INTO")) {
					line = line.substring(line.indexOf("("));
					for (String part : line.split("\\),\\(")) {

						String[] parts = part.split(",");

						try {
							int nameSpace = Integer.valueOf(parts[1]);
							if (nameSpace != 0)
								continue;
						} catch (NumberFormatException e) {
							System.out.println("WikipediaEventsByCategoryNameLoader: Ignore invalid namespace "
									+ parts[1] + " in " + part + ".");
							continue;
						}

						String p0 = parts[0];
						if (p0.startsWith("("))
							p0 = p0.substring(1);

						if (p0.isEmpty())
							continue;
						if (parts[2].isEmpty())
							continue;

						int pageId = Integer.valueOf(p0);
						try {
							String targetTitle = parts[2].substring(1, parts[2].length() - 1);
							if (redirectsWithIds.containsKey(pageId)) {
								eventPages.put(targetTitle, redirectsWithIds.get(pageId));
							}
						} catch (StringIndexOutOfBoundsException e) {
							continue;
						}

					}
				}
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

		return eventPages;
	}

	private static Map<Integer, String> loadEventPages(Language language) {
		Map<Integer, String> eventPageIDs = new HashMap<Integer, String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIPEDIA_CATEGORYLINKS, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String prevPart = null;
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("INSERT INTO")) {
					line = line.substring(line.indexOf("("));
					for (String part : line.split("\\),\\(")) {

						String[] parts = part.split(",");

						if (parts.length <= 2) {
							System.out.println("Problem with WIKIPEDIA_CATEGORYLINKS (" + language + "): " + prevPart
									+ " | " + part);
							continue;
						}

						prevPart = part;

						String pageIdString = parts[0];
						if (pageIdString.startsWith("("))
							pageIdString = pageIdString.substring(1);
						int pageId = Integer.valueOf(pageIdString);

						String categoryName = parts[1].substring(1, parts[1].length() - 1);

						boolean isEvent = false;
						for (Pattern pattern : WikiWords.getInstance().getEventCategoryRegexes(language)) {
							if (pattern.matcher(categoryName).matches()) {
								isEvent = true;
								break;
							}
						}

						// very specific special case. Ignore category
						// "Films_based_on_actual_events". TODO: Add this as
						// blacklist property to the configuration file.
						if (categoryName.contains("based_on") || categoryName.contains("based on"))
							isEvent = false;

						if (isEvent) {
							eventPageIDs.put(pageId, categoryName);
						}

					}
				}
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

		System.out.println("Found " + eventPageIDs.size() + " Wikipedia (" + language + ") category name events.");

		return eventPageIDs;
	}

	public static Map<String, String> getRedirectsDummy(Language language) {
		return new HashMap<String, String>();
	}

}
