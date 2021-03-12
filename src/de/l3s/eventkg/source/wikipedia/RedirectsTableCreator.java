package de.l3s.eventkg.source.wikipedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RedirectsTableCreator {

	public static void main(String[] args) {

		Config.init("config_eventkb_local.txt");

		// Map<Integer, String> redirectsWithIds = loadRedirects(Language.DE);
		// System.out.println(redirectsWithIds.keySet().size());
		// addLabelsToRedirects(redirectsWithIds, Language.DE);

		Map<String, String> redirects = getRedirects(Language.PT);

		System.out.println("Olbia: " + redirects.get("Avachinsky"));
	}

	public static Map<String, String> getRedirects(Language language) {
		System.out.println("Load redirects.");
		Map<Integer, String> redirectsWithIds = loadRedirects(language);
		System.out.println(redirectsWithIds.keySet().size() + " redirects. Load labels of redirect pages.");
		Map<String, String> redirects = addLabelsToRedirects(redirectsWithIds, language);
		System.out.println("Done loading redirects.");
		return redirects;
	}

	private static Map<String, String> addLabelsToRedirects(Map<Integer, String> redirectsWithIds, Language language) {

		Map<String, String> redirects = new HashMap<String, String>();

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

						List<String> parts = splitInsertLine(part);

						int nameSpace = Integer.valueOf(parts.get(1));
						if (nameSpace != 0)
							continue;

						String p0 = parts.get(0);
						if (p0.startsWith("("))
							p0 = p0.substring(1);

						if (p0.isEmpty())
							continue;
						if (parts.get(2).isEmpty())
							continue;

						int pageId = Integer.valueOf(p0);

						try {
							String targetTitle = parts.get(2).substring(1, parts.get(2).length() - 1);

							if (redirectsWithIds.containsKey(pageId)) {
								redirects.put(targetTitle, redirectsWithIds.get(pageId));
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

		return redirects;
	}

	private static Map<Integer, String> loadRedirects(Language language) {

		Map<Integer, String> redirects = new HashMap<Integer, String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIPEDIA_REDIRECTS, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String prevPart = null;
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("INSERT INTO")) {
					line = line.substring(line.indexOf("("));
					for (String part : line.split("\\),\\(")) {

						List<String> parts = splitInsertLine(part);

						if (parts.get(2).length() <= 1) {
							System.out.println(
									"Problem with WIKIPEDIA_REDIRECTS (" + language + "): " + prevPart + " | " + part);
							continue;
						}

						String pageIdString = parts.get(0);
						if (pageIdString.startsWith("("))
							pageIdString = pageIdString.substring(1);
						int pageId = Integer.valueOf(pageIdString);

						String targetTitle = parts.get(2).substring(1, parts.get(2).length() - 1);

						redirects.put(pageId, targetTitle);

						prevPart = part;
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

		return redirects;
	}

	public static Map<String, String> getRedirectsDummy(Language language) {
		return new HashMap<String, String>();
	}

	private static List<String> splitInsertLine(String line) {
		String[] partsTmp = line.split(",");

		List<String> parts = new ArrayList<String>();

		// remerge comma-separated values, if they are within
		// quotes. For example, 'Rom,_offene Stadt'
		String prefix = null;
		for (String subPart : partsTmp) {
			if (subPart.startsWith("'") && !subPart.endsWith("'"))
				prefix = subPart;
			else if (prefix != null & !subPart.endsWith("'"))
				prefix += "," + subPart;
			else {
				if (prefix != null)
					subPart = prefix + "," + subPart;
				parts.add(subPart);
				prefix = null;
			}
		}

		return parts;
	}

}
