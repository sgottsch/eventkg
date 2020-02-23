package de.l3s.eventkg.extension;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.source.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.EventDateExpressionsAll;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.EventExtractorFromYearPages;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.Output;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.TextExtractorNew;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.Link;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.Sentence;
import de.l3s.eventkg.util.FileLoader;
import edu.stanford.nlp.util.StringUtils;

public class LanguageAdderMetaFiles {

	public static void main(String[] args) throws IOException {

		// System.out.println(getLanguagePage(Language.EN, Language.DE,
		// "Elephant"));

		// List<String> pageTitles = new ArrayList<String>();
		// pageTitles.add(getLanguagePage(Language.EN, Language.EN, "2019"));

		// Map<String, List<String>> events = getEvents(getTerms(Language.EN),
		// getDateExpressions(Language.EN), pageTitles,
		// Language.EN);
		// for (String event : events.keySet()) {
		// System.out.println(event);
		// for (String line : events.get(event))
		// System.out.println(" " + line);
		// }

		Output textOutput = getText(getTerms(Language.HI), getDateExpressions(Language.DE), "Barack Obama",
				Language.DE);
		System.out.println(textOutput.getFirstSentence());
		for (Sentence sentence : textOutput.getSentences()) {
			System.out.println(sentence.getText());
			for (Link link : sentence.getLinks())
				System.out.println(" " + link.getAnchorText() + " -> " + link.getName());
		}
	}

	public static String getDateExpressions(Language language) throws IOException {

		// TODO: If file not exists -> english

		// If file not exists -> English
		File file = null;
		try {
			file = new File(LanguageAdderMetaFiles.class
					.getResource(
							"/resource/meta_data/wikipedia/" + language.getLanguage() + "/event_date_expressions.txt")
					.getFile());
		} catch (NullPointerException e) {
			file = new File(LanguageAdderMetaFiles.class.getResource(
					"/resource/meta_data/wikipedia/" + Language.EN.getLanguage() + "/event_date_expressions.txt")
					.getFile());
		}

		return FileLoader.readFile(file);

	}

	public static String getTerms(Language language) throws IOException {

		// If file not exists -> English
		File file = null;
		try {
			file = new File(LanguageAdderMetaFiles.class
					.getResource("/resource/meta_data/wikipedia/" + language.getLanguage() + "/words.txt").getFile());
			return FileLoader.readFile(file);
		} catch (NullPointerException e) {
			file = new File(LanguageAdderMetaFiles.class
					.getResource("/resource/meta_data/wikipedia/" + Language.EN.getLanguage() + "/words.txt")
					.getFile());

			String fileContent = FileLoader.readFile(file);
			String newFileContent = "";
			String section = null;
			boolean addedNamespaces = false;
			boolean addedMonthNames = false;
			boolean addedWeekdayNames = false;
			for (String line : fileContent.split("\n")) {
				if (line.startsWith("#")) {
					section = line;
					newFileContent += line + "\n";
				} else if (section.equals("# forbiddenNameSpaces")) {
					if (!addedNamespaces) {
						addedNamespaces = true;
						for (String namespace : NamespaceNamesLoader.getNamespaces(language))
							if (!namespace.isEmpty())
								newFileContent += namespace + "\n";
						newFileContent += "\n";
					}
				} else if (section.equals("# monthNames")) {
					if (!addedMonthNames) {
						addedMonthNames = true;
						for (String namespace : NamespaceNamesLoader.getMonthNames(language))
							newFileContent += namespace + "\n";
						newFileContent += "\n";
					}
				} else if (section.equals("# weekdayNames")) {
					if (!addedWeekdayNames) {
						addedWeekdayNames = true;
						for (String namespace : NamespaceNamesLoader.getWeekdayNames(language))
							newFileContent += namespace + "\n";
						newFileContent += "\n";

					}
				} else
					newFileContent += line + "\n";
			}
			return newFileContent;
		}

	}

	public static String getPageList(Language language) {

		List<String> pages = new ArrayList<String>();

		String page1 = getLanguagePage(Language.EN, language, "2018");
		if (page1 != null)
			pages.add(page1);

		String page2 = getLanguagePage(Language.EN, language, "July 13");
		if (page2 != null)
			pages.add(page2);

		String page3 = getLanguagePage(Language.EN, language, "2018 in sports");
		if (page3 != null)
			pages.add(page3);

		return StringUtils.join(pages, "\n");
	}

	public static Map<String, List<String>> getEvents(String terms, String dateExpressions, List<String> pages,
			Language language) {

		WikiWords.getInstance().init(language, terms);
		EventDateExpressionsAll.getInstance().init(language, dateExpressions);

		return getEvents(pages, language);
	}

	public static Map<String, List<String>> getEvents(List<String> pages, Language language) {

		Map<String, List<String>> outputs = new LinkedHashMap<String, List<String>>();

		for (String pageTitle : pages) {

			List<String> output = new ArrayList<String>();
			outputs.put(pageTitle, output);

			try {
				String url = "https://" + language.getLanguage()
						+ ".wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles="
						+ URLEncoder.encode(pageTitle, "UTF-8") + "&rvslots=main";
				JSONObject json = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));

				json = json.getJSONObject("query").getJSONObject("pages");
				json = json.getJSONObject(json.names().getString(0));
				JSONArray arr = json.getJSONArray("revisions");
				json = arr.getJSONObject(0).getJSONObject("slots").getJSONObject("main");

				String text = json.getString("*");

				EventExtractorFromYearPages extr = new EventExtractorFromYearPages(text, 0, pageTitle, language,
						RedirectsTableCreator.getRedirectsDummy(language));

				if (!extr.isYearOrDayPage())
					output.add("ERROR: Could not identify page \"" + pageTitle
							+ "\" as year or day page. Maybe check \"# dayTitle\"?");
				else {
					try {
						extr.extractEvents();
					} catch (NullPointerException e) {
						System.out.println("Error");
						e.printStackTrace();
					}

					for (String line : extr.getEventsOutput().split("\n")) {
						String[] parts = line.split("\t");
						if (parts.length > 6) {
							String date = parts[5];
							if (!parts[5].equals(parts[6]))
								date = parts[5] + " — " + parts[6];
							date = date.replace("AD ", "");
							output.add("<span class='date'>" + date + "</span>: " + parts[7]);
						}
					}
				}

			} catch (JSONException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return outputs;
	}

	public static Output getText(String terms, String dateExpressions, String page, Language language) {

		WikiWords.getInstance().init(language, terms);
		EventDateExpressionsAll.getInstance().init(language, dateExpressions);

		return getText(page, language);
	}

	public static Output getText(String pageTitle, Language language) {

		try {
			String url = "https://" + language.getLanguage()
					+ ".wikipedia.org/w/api.php?action=query&prop=revisions&rvprop=content&format=json&titles="
					+ URLEncoder.encode(pageTitle, "UTF-8") + "&rvslots=main";
			JSONObject json = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));

			json = json.getJSONObject("query").getJSONObject("pages");
			json = json.getJSONObject(json.names().getString(0));
			JSONArray arr = json.getJSONArray("revisions");
			json = arr.getJSONObject(0).getJSONObject("slots").getJSONObject("main");

			String text = json.getString("*");

			TextExtractorNew extr = new TextExtractorNew(text, 0, true, language, pageTitle,
					RedirectsTableCreator.getRedirectsDummy(language));

			try {
				extr.extractLinks();
				extr.addSentencesToOutput();
			} catch (NullPointerException e) {
				System.out.println("Error");
				e.printStackTrace();
			}

			return extr.getOutput();

		} catch (JSONException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String getLanguagePage(Language sourceLanguage, Language targetLanguage, String pageTitle) {

		if (sourceLanguage == targetLanguage)
			return pageTitle;

		try {
			String query = "https://" + sourceLanguage.getLanguage() + ".wikipedia.org/w/api.php?action=query&titles="
					+ URLEncoder.encode(pageTitle, "UTF-8") + "&prop=langlinks&lllang=" + targetLanguage.getLanguage()
					+ "&format=json";
			System.out.println(query);
			JSONObject json = new JSONObject(IOUtils.toString(new URL(query), Charset.forName("UTF-8")));

			json = json.getJSONObject("query").getJSONObject("pages");
			JSONArray arr = json.getJSONObject(json.names().getString(0)).getJSONArray("langlinks");
			return arr.getJSONObject(0).getString("*");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			return null;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
