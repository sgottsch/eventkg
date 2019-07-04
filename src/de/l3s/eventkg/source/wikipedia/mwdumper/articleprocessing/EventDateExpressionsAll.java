package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventDateExpressionsAll {

	private static EventDateExpressionsAll instance;

	private Language language;

	private String regexMonth;
	private String regexWeekdays;

	private List<Set<String>> monthNames;

	private Pattern dayTitle;

	private Set<Pattern> dateTemplatePatterns;

	private List<String> hyphens;

	private List<Pattern> yearTitlePatternsBC;

	private List<Pattern> yearTitlePatterns;

	private Map<String, List<String>> entriesPerType;

	private Map<String, String> placeHolders = new HashMap<String, String>();

	private Pattern placeholderPattern = Pattern.compile("@((?!@).)*@");

	public static EventDateExpressionsAll getInstance() {
		if (instance == null) {
			instance = new EventDateExpressionsAll();
		}
		return instance;
	}

	private EventDateExpressionsAll() {
	}

	public void init(Language language) {
		this.language = language;

		this.monthNames = WikiWords.getInstance().getMonthNames(language);
		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		this.regexWeekdays = WikiWords.getInstance().getWeekdayRegex(language);
		String regexMonth1 = "(?<m1>" + this.regexMonth.substring(1);
		this.placeHolders.put("regexMonth1", regexMonth1);

		String regexDay1 = "(?<d1>[1-3]?[0-9])";
		// TODO: Use something more stable (enums?)
		this.placeHolders.put("regexDay1", regexDay1);

		// en: January 22
		// de: 22. Januar
		// fr: 22 janvier
		// pt: 22 de janeiro
		// ru: 22 января

		// ---

		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		String regexMonth2 = "(?<m2>" + this.regexMonth.substring(1);
		this.placeHolders.put("regexMonth2", regexMonth2);

		this.hyphens = new ArrayList<String>();
		hyphens.add("-");
		hyphens.add("–");
		hyphens.add("—");
		hyphens.add("-");
		hyphens.add("—");
		hyphens.add("−");

		String hyphensOr = ("(" + StringUtils.join(hyphens, "|") + ")");
		this.placeHolders.put("hyphensOr", hyphensOr);
		String hyphensOrWithSlash = "(" + StringUtils.join(hyphens, "|") + "|/" + ")";
		this.placeHolders.put("hyphensOrWithSlash", hyphensOrWithSlash);

		String regexDay2 = "(?<d2>[1-3]?[0-9])";
		this.placeHolders.put("regexDay2", regexDay2);
		String regexYear = "([1-9][0-9]{2,3})";
		this.placeHolders.put("regexYear", regexYear);

		parseFile();

		// en: January 22
		// de: 22. Januar
		// fr: 22 janvier
		// pt: 22 de janeiro
		// ru: 22 января
		this.dayTitle = Pattern.compile(createPatternString(this.entriesPerType.get("dayTitle").get(0)));

		initYearTitlePatterns();
	}

	private void parseFile() {

		BufferedReader br = null;
		try {
			br = FileLoader.getReader(FileName.WIKIPEDIA_META_EVENT_DATE_EXPRESSIONS, language);

			this.entriesPerType = new HashMap<String, List<String>>();

			String line;
			String currentType = null;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty())
					continue;
				else if (line.startsWith("#"))
					currentType = line.split("\t")[0].substring(1).trim();
				else {
					if (!entriesPerType.containsKey(currentType)) {
						entriesPerType.put(currentType, new ArrayList<String>());
					}
					entriesPerType.get(currentType).add(line.trim());
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

		for (String line : entriesPerType.get("new regexes")) {
			String placeholderId = line.split("\t")[0].trim();
			placeholderId = placeholderId.substring(1, placeholderId.length() - 1);
			this.placeHolders.put(placeholderId, createPatternString(line.split("\t")[1].trim()));
		}

	}

	public String createPatternString(String fileInputLine) {

		Matcher m = placeholderPattern.matcher(fileInputLine);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String groupText = m.group();

			String placeHolder = groupText.substring(1, groupText.length() - 1);
			m.appendReplacement(sb, this.placeHolders.get(placeHolder));
		}
		m.appendTail(sb);

		return sb.toString();
	}

	private void initYearTitlePatterns() {

		// order important: first always try BC
		yearTitlePatternsBC = new ArrayList<Pattern>();
		yearTitlePatterns = new ArrayList<Pattern>();

		for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {
			for (String yearTitlePatternBC : this.entriesPerType.get("yearTitlePatternsBC")) {
				yearTitlePatternBC = yearTitlePatternBC.replaceAll("@digitsInYear@", String.valueOf(digitsInYear));
				yearTitlePatternsBC.add(Pattern.compile(createPatternString(yearTitlePatternBC)));
			}
			for (String yearTitlePattern : this.entriesPerType.get("yearTitlePatterns")) {
				yearTitlePattern = yearTitlePattern.replaceAll("@digitsInYear@", String.valueOf(digitsInYear));
				yearTitlePatterns.add(Pattern.compile(createPatternString(yearTitlePattern)));
			}
		}

	}

	public String getRegexMonth() {
		return regexMonth;
	}

	public String getRegexWeekdays() {
		return regexWeekdays;
	}

	public Integer getMonth(String monthName) {
		for (int i = 0; i < this.monthNames.size(); i++) {
			if (this.monthNames.get(i).contains(monthName)) {
				return Integer.valueOf(i + 1);
			}
		}

		return null;
	}

	public Pattern getDayTitle() {
		return dayTitle;
	}

	public Set<Pattern> getDateTemplatePatterns() {
		return dateTemplatePatterns;
	}

	public List<String> getHyphens() {
		return hyphens;
	}

	public List<Pattern> getYearTitlePatternsBC() {
		return yearTitlePatternsBC;
	}

	public List<Pattern> getYearTitlePatterns() {
		return yearTitlePatterns;
	}

	public Map<String, String> getPlaceHolders() {
		return placeHolders;
	}

	public String getPlaceHolder(String key) {
		return placeHolders.get(key);
	}

	public Map<String, List<String>> getEntriesPerType() {
		return entriesPerType;
	}

	public List<String> getEntriesPerType(String type) {
		return entriesPerType.get(type);
	}

}
