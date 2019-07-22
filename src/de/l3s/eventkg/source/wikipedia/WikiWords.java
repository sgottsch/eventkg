package de.l3s.eventkg.source.wikipedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import edu.stanford.nlp.util.StringUtils;

public class WikiWords {

	private static WikiWords instance;

	private Map<Language, Set<String>> forbiddenLinks = new HashMap<Language, Set<String>>();
	private Map<Language, Set<String>> forbiddenInternalLinks = new HashMap<Language, Set<String>>();
	private Map<Language, Set<String>> titlesOfParagraphsNotToRead = new HashMap<Language, Set<String>>();
	private Map<Language, Set<String>> eventTitlesNotToUseAsCategory = new HashMap<Language, Set<String>>();

	private Map<Language, String> tocNames = new HashMap<Language, String>();

	private Map<Language, Set<String>> fileLabels = new HashMap<Language, Set<String>>();
	private Map<Language, String> categoryLabels = new HashMap<Language, String>();
	private Map<Language, String> templateLabels = new HashMap<Language, String>();
	private HashMap<Language, Set<String>> imageLabels = new HashMap<Language, Set<String>>();
	private HashMap<Language, Set<String>> listPrefixes = new HashMap<Language, Set<String>>();
	private HashMap<Language, Set<String>> templatePrefixes = new HashMap<Language, Set<String>>();
	private HashMap<Language, Set<String>> categoryPrefixes = new HashMap<Language, Set<String>>();
	private Map<Language, Set<String>> eventsLabels = new HashMap<Language, Set<String>>();

	private Map<Language, String> monthRegex = new HashMap<Language, String>();
	private Map<Language, String> weekdayRegex = new HashMap<Language, String>();

	private Map<Language, Set<String>> seeAlsoLinkClasses = new HashMap<Language, Set<String>>();

	private Map<Language, Set<Pattern>> eventCategoryRegexes = new HashMap<Language, Set<Pattern>>();

	private Set<String> forbiddenImages;

	private Map<Language, List<Set<String>>> monthNames = new HashMap<Language, List<Set<String>>>();
	private Map<Language, List<Set<String>>> weekdayNames = new HashMap<Language, List<Set<String>>>();

	public static WikiWords getInstance() {
		if (instance == null) {
			instance = new WikiWords();
		}
		return instance;
	}

	private WikiWords() {
	}

	public void init(Language language) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(language);
		init(languages);
	}

	public void init(List<Language> languages) {

		for (Language language : languages) {

			this.eventTitlesNotToUseAsCategory.put(language, new HashSet<String>());

			BufferedReader br = null;
			try {
				br = FileLoader.getReader(FileName.WIKIPEDIA_META_WORDS, language);

				Map<String, List<Set<String>>> entriesPerType = new HashMap<String, List<Set<String>>>();
				Map<String, String> singleEntriesPerType = new HashMap<String, String>();
				String line;
				String currentType = null;
				while ((line = br.readLine()) != null) {
					if (line.isEmpty())
						continue;
					else if (line.startsWith("#"))
						currentType = line.substring(1).trim();
					else {
						if (!entriesPerType.containsKey(currentType)) {
							entriesPerType.put(currentType, new ArrayList<Set<String>>());
						}
						Set<String> words = new HashSet<String>();
						entriesPerType.get(currentType).add(words);
						for (String word : line.trim().split(";"))
							words.add(word);
						// if (singleEntriesPerType.containsKey(currentType)) {
						// throw new IllegalArgumentException(currentType + "
						// may only have one value.");
						// } else
						singleEntriesPerType.put(currentType, line.trim());
					}
				}

				String talkSuffix = null;
				String talkPrefix = null;
				Set<String> forbiddenNameSpaces = new HashSet<String>();
				Set<String> forbiddenInternalLinksOfLanguage = new HashSet<String>();

				this.forbiddenInternalLinks.put(language, forbiddenInternalLinksOfLanguage);

				for (String type : entriesPerType.keySet()) {
					switch (type) {
					case "forbiddenLinks":
						addValues(this.forbiddenLinks, language, entriesPerType, type);
						break;
					case "tableOfContents":
						this.tocNames.put(language, singleEntriesPerType.get(type));
						break;
					case "seeAlsoLinkClasses":
						addValues(this.seeAlsoLinkClasses, language, entriesPerType, type);
						break;
					case "titlesOfParagraphsNotToRead":
						addValues(this.titlesOfParagraphsNotToRead, language, entriesPerType, type);
						break;
					case "eventTitlesNotToUseAsCategory":
						addValues(this.eventTitlesNotToUseAsCategory, language, entriesPerType, type);
						break;
					case "fileLabels":
						addValues(this.fileLabels, language, entriesPerType, type);
						break;
					case "talkSuffix":
						if (singleEntriesPerType.containsKey(type))
							talkSuffix = singleEntriesPerType.get(type);
						break;
					case "talkPrefix":
						if (singleEntriesPerType.containsKey(type))
							talkPrefix = singleEntriesPerType.get(type);
						break;
					case "forbiddenNameSpaces":
						for (Set<String> values : entriesPerType.get(type)) {
							for (String value : values) {
								forbiddenNameSpaces.add(value);
							}
						}
						break;
					case "forbiddenInternalLinks":
						for (Set<String> values : entriesPerType.get(type)) {
							for (String value : values) {
								forbiddenInternalLinksOfLanguage.add(value);
							}
						}
						break;
					case "categoryLabel":
						this.categoryLabels.put(language, singleEntriesPerType.get(type));
						break;
					case "templateLabel":
						this.templateLabels.put(language, singleEntriesPerType.get(type));
						break;
					case "imageLabels":
						addValues(this.imageLabels, language, entriesPerType, type);
						break;
					case "listPrefixes":
						addValues(this.listPrefixes, language, entriesPerType, type);
						break;
					case "categoryPrefixes":
						addValues(this.categoryPrefixes, language, entriesPerType, type);
						break;
					case "templatePrefixes":
						addValues(this.templatePrefixes, language, entriesPerType, type);
						break;
					case "eventsLabels":
						addValues(this.eventsLabels, language, entriesPerType, type);
						break;
					case "monthNames":
						addValuesInOrder(this.monthNames, language, entriesPerType, type);
						break;
					case "weekdayNames":
						addValuesInOrder(this.weekdayNames, language, entriesPerType, type);
						break;
					case "eventCategoryRegexes":
						if (this.eventCategoryRegexes == null)
							this.eventCategoryRegexes = new HashMap<Language, Set<Pattern>>();
						this.eventCategoryRegexes.put(language, new HashSet<Pattern>());
						for (Set<String> values : entriesPerType.get(type)) {
							for (String value : values)
								this.eventCategoryRegexes.get(language).add(Pattern.compile(value.replace(" ", "_")));
						}
						break;
					default:
						break;
					}
				}

				for (String forbiddenNameSpace : forbiddenNameSpaces) {
					forbiddenInternalLinksOfLanguage.add(String.valueOf(forbiddenNameSpace) + ":");

					if (talkSuffix != null) {
						forbiddenInternalLinksOfLanguage
								.add(String.valueOf(forbiddenNameSpace) + " " + talkSuffix + ":");
						continue;
					}

					if (talkPrefix != null) {
						forbiddenInternalLinksOfLanguage
								.add(String.valueOf(talkPrefix) + " " + forbiddenNameSpace + ":");
						forbiddenInternalLinksOfLanguage
								.add(String.valueOf(talkPrefix) + " " + forbiddenNameSpace.substring(0, 1).toLowerCase()
										+ forbiddenNameSpace.substring(1) + ":");
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
		}

	}

	private void addValuesInOrder(Map<Language, List<Set<String>>> map, Language language,
			Map<String, List<Set<String>>> entriesPerType, String type) {

		map.put(language, new ArrayList<Set<String>>());

		for (Set<String> values : entriesPerType.get(type)) {
			Set<String> aliases = new HashSet<String>();
			for (String alias : values)
				aliases.add(alias);
			map.get(language).add(aliases);
		}
	}

	private void addValues(Map<Language, Set<String>> map, Language language,
			Map<String, List<Set<String>>> entriesPerType, String type) {
		map.put(language, new HashSet<String>());
		for (Set<String> values : entriesPerType.get(type)) {
			for (String value : values)
				map.get(language).add(value);
		}
	}

	// private void addValuesInOrder(Map<Language, List<String>> map, Language
	// language,
	// Map<String, List<String>> entriesPerType, String type) {
	// map.put(language, new ArrayList<String>());
	// for (String value : entriesPerType.get(type)) {
	// map.get(language).add(value);
	// }
	// }

	public Set<String> getForbiddenInternalLinks(Language language) {
		return this.forbiddenInternalLinks.get(language);
	}

	public Set<String> getForbiddenLinks(Language language) {
		return this.forbiddenLinks.get(language);
	}

	public Set<String> getTitlesOfParagraphsNotToRead(Language language) {
		return titlesOfParagraphsNotToRead.get(language);
	}

	public Set<String> getEventTitlesNotToUseAsCategory(Language language) {
		return eventTitlesNotToUseAsCategory.get(language);
	}

	public String getTOCName(Language language) {
		return this.tocNames.get(language);
	}

	public Set<String> getSeeAlsoLinkClasses(Language language) {
		return seeAlsoLinkClasses.get(language);
	}

	public Set<String> getForbiddenImages() {
		if (this.forbiddenImages == null) {
			this.forbiddenImages = new HashSet<String>();
			// Source: http://wikimediafoundation.org/wiki/Wikimedia_trademarks

			forbiddenImages.add("Commons-logo.svg");
			forbiddenImages.add("WiktionaryEn.svg");
			forbiddenImages.add("Wiktionary-logo-en.svg");
			forbiddenImages.add("Wikiquote-logo.svg");
			forbiddenImages.add("WiktionaryEn.svg");
			forbiddenImages.add("Wikiquote-logo-en.svg");
			forbiddenImages.add("Wikibooks-logo.svg");
			forbiddenImages.add("Wikibooks-logo-en-noslogan.svg");
			forbiddenImages.add("Wikisource-logo.svg");
			forbiddenImages.add("Wikisource-newberg-de.png");
			forbiddenImages.add("Wikinews-logo.svg");
			forbiddenImages.add("WikiNews-Logo-en.svg");
			forbiddenImages.add("Wikiversity-logo.svg");
			forbiddenImages.add("Wikiversity-logo-en.svg");
			forbiddenImages.add("Wikispecies-logo.svg");
			forbiddenImages.add("WikiSpecies.svg");
			forbiddenImages.add("MediaWiki-notext.svg");
			forbiddenImages.add("MediaWiki.svg");
			forbiddenImages.add("Commons-logo.svg");
			forbiddenImages.add("Commons-logo-en.svg");
			forbiddenImages.add("Wikidata-logo.svg");
			forbiddenImages.add("Wikidata-logo-en.svg");
			forbiddenImages.add("Wikivoyage-Logo-v3-icon.svg");
			forbiddenImages.add("Wikivoyage-Logo-v3-en.svg");
			forbiddenImages.add("Incubator-notext");
			forbiddenImages.add("Incubator-text.svg");
			forbiddenImages.add("Wikimedia_labs_logo.svg");
			forbiddenImages.add("Wikimedia_labs_logo_with_text.svg");
			forbiddenImages.add("Wikimedia-logo.svg");
			forbiddenImages.add("Wmf_logo_vert_pms.svg");
			forbiddenImages.add("Wikimania.svg");
			forbiddenImages.add("Wikimania_logo_with_text_2.svg");

			// Others
			// TODO: Continue...

			forbiddenImages.add("Ambox_important.svg");
			forbiddenImages.add("Question_book.svg");
			forbiddenImages.add("Portal_icon.svg");

		}

		return this.forbiddenImages;

	}

	public static String getWikiImageUrlBegin() {
		return "//upload.wikimedia.org";
	}

	public Set<String> getFileLabel(Language language) {
		return fileLabels.get(language);
	}

	public String getCategoryLabel(Language language) {
		return categoryLabels.get(language);
	}

	public String getTemplateLabel(Language language) {
		return templateLabels.get(language);
	}

	public Set<String> getEventsLabels(Language language) {
		return eventsLabels.get(language);
	}

	public Set<String> getImageLabels(Language language) {
		return imageLabels.get(language);
	}

	public Set<String> getListPrefixes(Language language) {
		return listPrefixes.get(language);
	}

	public Set<String> getTemplatePrefixes(Language language) {
		return templatePrefixes.get(language);
	}

	public Set<String> getCategoryPrefixes(Language language) {
		return categoryPrefixes.get(language);
	}

	public Set<Pattern> getEventCategoryRegexes(Language language) {
		return eventCategoryRegexes.get(language);
	}

	public List<Set<String>> getMonthNames(Language language) {
		return this.monthNames.get(language);
	}

	public List<Set<String>> getWeekdayNames(Language language) {
		return weekdayNames.get(language);
	}

	public String getMonthRegex(Language language) {

		if (this.monthRegex == null)
			this.monthRegex = new HashMap<Language, String>();
		if (!this.monthRegex.containsKey(language)) {

			List<String> regexParts = new ArrayList<String>();

			for (Set<String> names : getMonthNames(language)) {
				regexParts.add("(" + StringUtils.join(names, "|") + ")");
			}

			this.monthRegex.put(language, "(" + StringUtils.join(regexParts, "|") + ")");
		}

		return this.monthRegex.get(language);
	}

	public String getWeekdayRegex(Language language) {

		if (this.weekdayRegex == null)
			this.weekdayRegex = new HashMap<Language, String>();
		if (!this.weekdayRegex.containsKey(language)) {

			List<String> regexParts = new ArrayList<String>();

			for (Set<String> names : getWeekdayNames(language)) {
				regexParts.add("(" + StringUtils.join(names, "|") + ")");
			}

			this.weekdayRegex.put(language, "(" + StringUtils.join(regexParts, "|") + ")");
		}

		return this.weekdayRegex.get(language);
	}

}
