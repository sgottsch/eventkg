package de.l3s.eventkg.wikipedia.mwdumper.articleprocessing;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.wikipedia.WikiWords;
import de.l3s.eventkg.wikipedia.mwdumper.model.event.Event;
import de.l3s.eventkg.wikipedia.mwdumper.model.event.LineNode;
import de.l3s.eventkg.wikipedia.mwdumper.model.event.PartialDate;

public class EventExtractorFromYearPages {
	private String text;
	private int pageId;
	private Integer year;
	private Integer month;
	private Integer day;

	private String pageTitle;
	private String eventsOutput = "";

	private Language language;

	private List<Set<String>> monthNames;

	private List<String> hyphens;

	private List<Event> events;

	private SimpleDateFormat dateFormat;

	private String regexMonth;
	private String regexWeekdays;

	private String hyphensOr;

	private boolean isYearPage;

	private List<Pattern> dateLinkResolvers = new ArrayList<Pattern>();

	private List<DatePattern> datePatterns;

	private Map<String, String> redirects;
	private HashSet<Pattern> dateTemplatePatterns;

	public static void main(String[] args) throws IOException {

		Map<Language, Map<Integer, String>> exampleTexts = new HashMap<Language, Map<Integer, String>>();

		Config.init("config_eventkb_local.txt");

		Map<Integer, String> enMap = new HashMap<Integer, String>();
		enMap.put(72578, "Joab");
		enMap.put(737, "Afghanistan");
		enMap.put(58478, "Airborne forces");
		enMap.put(41915, "Primary market");
		enMap.put(76216, "List of regions of Quebec");
		enMap.put(79266, "Armor-piercing shell");
		enMap.put(35375, "123");
		enMap.put(160795, "1988");
		enMap.put(53347099, "Green Light (Lorde song)");
		enMap.put(2646680, "1989 in British music");
		exampleTexts.put(Language.EN, enMap);

		Map<Integer, String> deMap = new HashMap<Integer, String>();
		deMap.put(83, "Al Pacino");
		deMap.put(5909, "1957");
		deMap.put(499753, "2014");
		deMap.put(1531063, "2017");
		exampleTexts.put(Language.DE, deMap);

		Map<Integer, String> ruMap = new HashMap<Integer, String>();
		ruMap.put(46992, "Пачино, Аль");
		ruMap.put(2647, "1957 год");
		ruMap.put(160616, "2014 год");
		ruMap.put(4611, "4 октября");
		exampleTexts.put(Language.RU, ruMap);

		Map<Integer, String> frMap = new HashMap<Integer, String>();
		frMap.put(32864, "Ronald Reagan");
		frMap.put(3506, "1957");
		frMap.put(5335, "2014");
		frMap.put(5335, "2014");
		frMap.put(3627989, "2009 en Palestine");
		exampleTexts.put(Language.FR, frMap);

		Map<Integer, String> ptMap = new HashMap<Integer, String>();
		ptMap.put(13541, "Ronald Reagan");
		ptMap.put(24532, "2007");
		ptMap.put(29071, "3 a.C.");
		ptMap.put(28422, "2017");
		exampleTexts.put(Language.PT, ptMap);

		Language language = Language.FR;
		int id = 3627989;

		String text = IOUtils.toString(
				TextExtractorNew.class.getResourceAsStream("/resource/wikipage/" + language.getLanguage() + "/" + id),
				"UTF-8");

		EventExtractorFromYearPages extr = new EventExtractorFromYearPages(text, id, exampleTexts.get(language).get(id),
				language, RedirectsTableCreator.getRedirectsDummy(language));
		try {
			extr.extractEvents();
		} catch (NullPointerException e) {
			System.out.println("Error");
			e.printStackTrace();
		}

		System.out.println(extr.getEventsOutput());
	}

	public EventExtractorFromYearPages(String text, int pageId, String title, Language language,
			Map<String, String> redirects) {
		this.text = text;
		this.pageId = pageId;
		this.pageTitle = title;
		this.language = language;
		this.redirects = redirects;
		this.text = this.text.replaceAll("'''", "");
		this.text = this.text.replaceAll("''", "");
		this.text = this.text.replaceAll("&nbsp;", " ");
		this.text = this.text.replaceAll("&ndash;", "–");
		this.text = this.text.replaceAll("&mdash;", "—");
		this.text = this.text.replaceAll("\\{\\{0\\}\\}", "");

		identifyDayPage(title);

		if (this.month == null)
			this.year = extractYearFromTitle(title);

		if (this.year == null && this.month == null) {
			this.isYearPage = false;
			return;
		}

		this.isYearPage = true;
		this.eventsOutput = "";
		init();
	}

	private void identifyDayPage(String title) {

		this.monthNames = WikiWords.getInstance().getMonthNames(language);

		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		this.regexWeekdays = WikiWords.getInstance().getWeekdayRegex(language);
		String regexMonth1 = "(?<m1>" + this.regexMonth.substring(1);

		String regexDay1 = "(?<d1>[1-3]?[0-9])";

		// en: January 22
		// de: 22. Januar
		// fr: 22 janvier
		// pt: 22 de janeiro
		// ru: 22 января

		Pattern dayTitle = null;
		if (language == Language.EN) {
			dayTitle = Pattern.compile("^" + regexMonth1 + " " + regexDay1 + "$");
		} else if (language == Language.DE) {
			dayTitle = Pattern.compile("^" + regexDay1 + "\\. " + regexMonth1 + "$");
		} else if (language == Language.FR) {
			dayTitle = Pattern.compile("^" + regexDay1 + " " + regexMonth1 + "$");
		} else if (language == Language.RU) {
			dayTitle = Pattern.compile("^" + regexDay1 + " " + regexMonth1 + "$");
		} else if (language == Language.PT) {
			dayTitle = Pattern.compile("^" + regexDay1 + " de " + regexMonth1 + "$");
		}

		MatcherResult mRes = match(dayTitle, title);
		if (mRes.getDatePart() != null) {
			this.day = Integer.valueOf(mRes.getMatcher().group("d1"));
			this.month = getMonth(mRes.getMatcher().group("m1")).intValue();
		}

	}

	public boolean isYearPage() {
		return this.isYearPage;
	}

	private Integer extractYearFromTitle(String title) {
		// if ((!title.endsWith(" BC")) && (!title.contains(" BC "))) {

		// order important: first always try BC
		List<Pattern> patternsBC = new ArrayList<Pattern>();
		List<Pattern> patterns = new ArrayList<Pattern>();

		if (language == Language.EN) {
			for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {

				// Examples: 2015, 2015 in science, 2015 in Germany, 2015 in
				// badminton, 2015 in television, January 17

				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) BC$"));
				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) BC in .*$"));

				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "})$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) in .*$"));
			}
		} else if (language == Language.DE) {

			// Examples: 2015, 17. Januar, Literaturjahr 1856, Filmjahr 1907,
			// Rundfunkjar 1987

			for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {
				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) v\\. ?Chr\\."));
				patternsBC.add(Pattern.compile("^.*jahr (?<y>[0-9]{" + digitsInYear + "}) v\\. ?Chr\\.$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "})$"));
				patterns.add(Pattern.compile("^.*jahr (?<y>[0-9]{" + digitsInYear + "})$"));
			}
		} else if (language == Language.FR) {

			// Examples: 1856, 1856 en littérature, Terrorisme avant 1946,
			// Décembre 1800, 24 décembre, 2011 par pays en Amérique

			String conjunctions = "(dans|en|au|avant|chez|aux|à|par pays)";

			for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {
				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) av\\. ?J\\.-C\\.$"));
				patternsBC.add(Pattern
						.compile("^(?<y>[0-9]{" + digitsInYear + "}) av\\. ?J\\.-C\\. " + conjunctions + " .*$"));

				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "})$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) " + conjunctions + " .*$"));
			}
		} else if (language == Language.PT) {

			// Examples: 2015, 7 de janeiro, 2015 no cinema, 2015 na literatura,
			// 2015 na ciência, Mortes em 2015

			String conjunctions = "(no|na|em)";

			for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) a\\. ?C\\.$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) " + conjunctions + " a\\. ?C\\.$"));

				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "})$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) " + conjunctions + " .*$"));
			}
		} else if (language == Language.RU) {

			// Examples: 8 января, 2015 год, 2015 год в спорте

			for (int digitsInYear = 4; digitsInYear >= 1; digitsInYear--) {
				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) год до н\\. э\\."));
				patternsBC.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) год до н\\. э\\.  в .*$"));

				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) год$"));
				patterns.add(Pattern.compile("^(?<y>[0-9]{" + digitsInYear + "}) год в .*$"));
			}
		}

		for (Pattern p : patternsBC) {
			Matcher m = p.matcher(title);
			if (m.matches()) {
				String text = m.group("y");
				if (!text.startsWith("0")) {
					int year = -Integer.parseInt(text);
					return year;
				}
			}
		}
		for (Pattern p : patterns) {
			Matcher m = p.matcher(title);
			if (m.matches()) {
				String text = m.group("y");
				if (!text.startsWith("0")) {
					int year = Integer.parseInt(text);
					if (year <= 2100)
						return year;
				}
			}
		}

		return null;
		// throw new ParseException("Could not find a year in " + title + ".",
		// 0);
	}

	private void init() {

		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		String regexMonth1 = "(?<m1>" + this.regexMonth.substring(1);
		String regexMonth2 = "(?<m2>" + this.regexMonth.substring(1);

		this.hyphens = new ArrayList<String>();
		this.hyphens.add("-");
		this.hyphens.add("–");
		this.hyphens.add("—");
		this.hyphens.add("-");
		this.hyphens.add("—");

		this.hyphensOr = ("(" + StringUtils.join(this.hyphens, "|") + ")");
		String hyphensOrWithSlash = "(" + StringUtils.join(this.hyphens, "|") + "|/" + ")";

		this.dateFormat = new SimpleDateFormat("G yyyy-MM-dd");

		String regexDay1 = "(?<d1>[1-3]?[0-9])";
		String regexDay2 = "(?<d2>[1-3]?[0-9])";
		String regexYear = "([1-9][0-9]{2,3})";

		this.datePatterns = new ArrayList<DatePattern>();

		if (this.month != null)
			this.datePatterns.add(
					new DatePattern(Pattern.compile("^(?<y1>" + regexYear + ")"), false, false, false, false, true));

		this.dateTemplatePatterns = new HashSet<Pattern>();
		Pattern dateTemplatePattern1 = Pattern
				.compile("^\\{\\{date\\|" + regexDay1 + "\\|" + regexMonth1 + "\\|(?<y1>" + regexYear + ")\\}\\}");
		this.dateTemplatePatterns.add(dateTemplatePattern1);
		this.datePatterns.add(new DatePattern(dateTemplatePattern1, true, false, true, false, true));

		if (language == Language.EN) {

			String regexDayMonth1 = regexDay1 + " " + regexMonth1;
			String regexDayMonth2 = regexDay2 + " " + regexMonth2;
			String regexMonthDay1 = regexMonth1 + " " + regexDay1;
			String regexMonthDay2 = regexMonth2 + " " + regexDay2;

			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);

			Pattern datePatternMonthDayCommaYear = Pattern.compile("^" + regexMonthDay1 + ", ?" + regexYear);

			Pattern datePatternDayMonthCommaYear = Pattern.compile("^" + regexDayMonth1 + ", ?" + regexYear);

			Pattern datePatternMonthDay = Pattern.compile("^" + regexMonthDay1);

			Pattern datePatternMonthHyphenMonth = Pattern
					.compile("^" + regexMonth1 + " ?" + hyphensOrWithSlash + " ?" + regexMonth2);
			this.datePatterns.add(new DatePattern(datePatternMonthHyphenMonth, false, false, true, true));

			Pattern datePatternMonthDayHyphenMonthDay = Pattern
					.compile("^" + regexMonthDay1 + this.hyphensOr + regexMonthDay2);
			this.datePatterns.add(new DatePattern(datePatternMonthDayHyphenMonthDay, true, true, true, true));

			Pattern datePatternDayMonthHyphenDayMonth = Pattern
					.compile("^" + regexDayMonth1 + this.hyphensOr + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true, true));

			Pattern datePatternDayMonthHyphenDay = Pattern
					.compile("^" + regexDayMonth1 + " ?" + this.hyphensOr + " ?" + regexDay2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDay, true, true, true, false));

			Pattern datePatternMonthDayHyphenDay = Pattern
					.compile("^" + regexMonthDay1 + " ?" + this.hyphensOr + " ?" + regexDay2);
			this.datePatterns.add(new DatePattern(datePatternMonthDayHyphenDay, true, true, true, false));

			this.datePatterns.add(new DatePattern(datePatternMonthDayCommaYear, true, false, true, false));
			this.datePatterns.add(new DatePattern(datePatternDayMonthCommaYear, true, false, true, false));
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));
			this.datePatterns.add(new DatePattern(datePatternMonthDay, true, false, true, false));

			Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternMonth, false, false, true, false));

			Pattern datePatternDay = Pattern.compile("^" + regexDay1);
			this.datePatterns.add(new DatePattern(datePatternDay, true, false, false, false));

			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexMonth1 + " " + regexDay1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " + regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexMonth1 + " " + regexDay1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " " + regexMonth1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")\\]\\]"));
		} else if (language == Language.DE) {

			String regexDayMonth1 = regexDay1 + "\\. " + regexMonth1;
			String regexDayMonth2 = regexDay2 + "\\. " + regexMonth2;

			String hyphensOrWithSlashAndBis = "(" + StringUtils.join(this.hyphens, "|") + "|/|(bis)" + ")";

			Pattern datePatternDayMonthHyphenDayMonth = Pattern
					.compile("^" + regexDayMonth1 + " ?" + hyphensOrWithSlashAndBis + " ?" + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true, true));
			Pattern datePatternDayHyphenDayMonth = Pattern
					.compile("^" + regexDay1 + "\\. ?" + hyphensOrWithSlashAndBis + " ?" + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayHyphenDayMonth, true, true, false, true));
			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1 + "( " + year + ")?");
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));
			Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternMonth, false, false, true, false));
			Pattern datePatternDayFrom = Pattern.compile("^(Ab |ab )" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayFrom, true, false, true, false));
			Pattern datePatternDayUntil = Pattern.compile("^(Bis |bis )" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayUntil, true, false, true, false));

			dateLinkResolvers
					.add(Pattern.compile("\\[\\[" + regexDay1 + "\\. " + regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + "\\. " + regexMonth1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")\\]\\]"));
		} else if (language == Language.FR) {

			String regexDayMonth1 = regexDay1 + " " + regexMonth1;

			String hyphensOrWithSlashAndText = "(" + StringUtils.join(this.hyphens, "|") + "|/|(et|au)" + ")";

			Pattern datePatternDayMonthDayMonth = Pattern.compile("^" + regexDay1 + " " + regexMonth1 + " ?"
					+ hyphensOrWithSlashAndText + " ?" + regexDay2 + " " + regexMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthDayMonth, true, true, true, true));
			Pattern datePatternDayDayMonth = Pattern
					.compile("^" + regexDay1 + " ?" + hyphensOrWithSlashAndText + " ?" + regexDay2 + " " + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayDayMonth, true, true, true, false));
			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));
			Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternMonth, false, false, true, false));

			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " + regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " " + regexMonth1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexMonth1 + " " + year + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")\\]\\]"));
		} else if (language == Language.PT) {

			String regexDayMonth1 = regexDay1 + "º? de " + regexMonth1;

			Pattern datePatternDayDayMonth = Pattern
					.compile("^" + regexDay1 + " (a|e) " + regexDay2 + " de " + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayDayMonth, true, true, true, false));
			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));

			dateLinkResolvers
					.add(Pattern.compile("\\[\\[" + regexDay1 + " de " + regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " de " + regexMonth1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")\\]\\]"));
		} else if (language == Language.RU) {

			String regexDayMonth1 = regexDay1 + " " + regexMonth1;
			String regexDayMonth2 = regexDay2 + " " + regexMonth2;

			Pattern datePatternDayMonthHyphenDayMonth = Pattern
					.compile("^" + regexDayMonth1 + " ?" + hyphensOrWithSlash + " ?" + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true, true));
			Pattern datePatternDayHyphenDayMonth = Pattern
					.compile("^" + regexDay1 + " ?" + hyphensOrWithSlash + " ?" + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayHyphenDayMonth, true, true, false, true));
			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));

			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " + regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " " + regexMonth1 + ")\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")( (год))?\\]\\]"));
		}
	}

	public void extractEvents() {
		boolean inEvents = false;

		LineNode root = null;
		LineNode currentNode = null;
		LineNode currentSectionNode = null;

		for (String line : this.text.split("\n")) {

			line = line.trim();
			String lineText = StringUtils.strip(line, "= ");

			// if (line.replace(" ", "").trim().startsWith("==") &&
			// !line.replace(" ", "").trim().startsWith("===")) {
			if (containsAny(line.replace(" ", "").trim(), WikiWords.getInstance().getEventsLabels(language))) {
				root = new LineNode(line, 0);
				root.setType(LineNode.NodeType.TITLE);
				root.getPartialDate().setYear(this.year);
				if (this.month != null)
					root.getPartialDate().addMonth(this.month);
				if (this.day != null)
					root.getPartialDate().addDay(this.day);
				currentNode = root;
				currentSectionNode = root;
				inEvents = true;
			}
			// else if (!inEvents && line.startsWith("=") && line.endsWith("="))
			// {
			// if (lineText.matches("^"+this.regexMonth+"$")) {
			// root = new LineNode(line, 0);
			// root.setType(LineNode.NodeType.TITLE);
			// root.getPartialDate().setYear(this.year);
			// if (this.month != null)
			// root.getPartialDate().addMonth(this.month);
			// if (this.day != null)
			// root.getPartialDate().addDay(this.day);
			// currentNode = root;
			// currentSectionNode = root;
			// inEvents = true;
			// }
			// }
			else {
				if (inEvents && line.trim().matches("^==[^=].*")) {
					inEvents = false;
					break;
				}
				if (inEvents && !line.isEmpty()) {
					if (line.startsWith("*") || line.startsWith("=")) {

						if (line.startsWith("=")) {
							int level = countOccurencesOfCharacter(line, Character.valueOf('=')) - 2;

							line = StringUtils.stripStart(line, "=");
							line = StringUtils.stripEnd(line, "=").trim();
							if (!line.isEmpty()) {
								LineNode node = new LineNode(line, level);
								node.setType(LineNode.NodeType.TITLE);
								if (currentNode.getLevel() == level) {
									currentNode.getParent().addChild(node);
								} else if (currentNode.getLevel() < level) {
									currentNode.addChild(node);
								} else {
									try {
										currentNode.getParentAtLevel(level - 1).addChild(node);
									} catch (NullPointerException e) {
										System.err.println("Error with page " + this.pageId + ": " + this.pageTitle);
										System.err.println(e.getMessage() + "\n" + e.getStackTrace());
									}
								}
								currentSectionNode = node;
								currentNode = node;
							}
						} else if (line.startsWith("*")) {
							int level = countOccurencesOfCharacter(line, Character.valueOf('*'));
							line = line
									.substring(countOccurencesOfCharacter(line, Character.valueOf('*')), line.length())
									.trim();
							level += currentSectionNode.getLevel();
							LineNode node = new LineNode(line, level);
							node.setType(LineNode.NodeType.LINE);
							if (currentNode.getLevel() == level) {
								currentNode.getParent().addChild(node);
							} else if (currentNode.getLevel() < level) {
								currentNode.addChild(node);
							} else {
								currentNode.getParentAtLevel(level - 1).addChild(node);
							}
							currentNode = node;
						}
					}
				}
			}
		}

		if (root == null) {
			return;
		}
		PartialDate rootDate = new PartialDate();
		root.setPartialDate(rootDate);

		if (this.year != null)
			root.getPartialDate().setYear(this.year);
		if (this.month != null) {
			root.getPartialDate().addMonth(this.month);
			root.getPartialDate().addDay(this.day);
		}
		extractDates(root, this.pageTitle.replace(">", "-"));

		this.events = new ArrayList<Event>();
		extractEvents(root);

		for (Event event : this.events) {
			boolean changed = true;
			while (changed) {

				changed = false;

				Pattern p = ReferenceAndTemplateRemover.getInstance(language).getLinksFindPattern();
				Matcher m = p.matcher(event.getRawText());

				StringBuffer sb = new StringBuffer();
				while (m.find()) {
					String linkName;
					changed = true;
					String anchorText = linkName = m.group().substring(2, m.group().length() - 2);

					boolean starts = false;
					for (String label : WikiWords.getInstance().getFileLabel(language))
						if (linkName.startsWith(label + ":"))
							starts = true;
					for (String label : WikiWords.getInstance().getImageLabels(language))
						if (linkName.startsWith(label + ":"))
							starts = true;

					if (starts) {
						m.appendReplacement(sb, "");
						continue;
					}
					if (linkName.contains("|")) {
						anchorText = linkName.substring(linkName.indexOf("|") + 1, linkName.length());
						linkName = linkName.substring(0, linkName.indexOf("|"));
					}
					String insertedAnchorText = Matcher.quoteReplacement(anchorText);
					m.appendReplacement(sb, insertedAnchorText);
					if (linkName.equals("#"))
						continue;

					linkName = resolveRedirects(linkName);
					if (linkName == null)
						continue;

					if (!linkName.contains("#"))
						event.addLink(linkName);
				}
				m.appendTail(sb);
				event.setRawText(sb.toString());
			}

		}

		this.eventsOutput = "";
		List<String> lines = new ArrayList<String>();
		for (Event event : this.events) {

			// Clean text again after link removal
			event.setRawText(cleanTextPart(event.getRawText()));
			if (event.getRawText() == null)
				continue;

			String line = getEventLine(event);
			if (line != null) {
				lines.add(line);
			}
		}

		this.eventsOutput = StringUtils.join(lines, Config.NL);
	}

	private boolean containsAny(String stack, Set<String> needles) {

		for (String needle : needles)
			if (stack.contains(needle))
				return true;

		return false;
	}

	private String resolveRedirects(String linkName) {
		linkName = linkName.replaceAll(" ", "_");

		if (linkName.isEmpty())
			return null;

		linkName = StringUtils.capitalize(linkName);
		linkName = linkName.replaceAll(Config.TAB, "_");

		if (this.redirects.containsKey(linkName)) {
			linkName = this.redirects.get(linkName);
		}

		return linkName;
	}

	private void extractDates(LineNode node, String titles) {

		node.setTitles(titles);

		String lineBefore = node.getLine();
		boolean foundNewDateInformation = extractPartialDateFromLine(node);

		if ((!foundNewDateInformation) && (node.getType() == LineNode.NodeType.TITLE)) {
			lineBefore = StringUtils.stripStart(lineBefore, "=");
			lineBefore = StringUtils.stripEnd(lineBefore, "=").trim();
		}

		for (LineNode child : node.getChildren()) {
			extractDates(child, titles);
		}
	}

	private void extractEvents(LineNode node) {
		Date startDate = null;
		Date endDate = null;
		if (node.getLine() != null && node.getType() != LineNode.NodeType.DATE
				&& node.getPartialDate().getYear() != null) {

			startDate = node.getPartialDate().transformToStartDate();
			endDate = node.getPartialDate().transformToEndDate();

			PartialDate.Granularity granularity = PartialDate.Granularity.YEAR;
			if (!node.getPartialDate().getDays().isEmpty()) {
				granularity = PartialDate.Granularity.DAY;
			} else if (!node.getPartialDate().getMonths().isEmpty()) {
				granularity = PartialDate.Granularity.MONTH;
			}
			if (node.getType() != LineNode.NodeType.TITLE) {
				Event event = new Event(startDate, endDate, node.getLine());
				event.setGranularity(granularity);
				event.setOriginalText(node.getOriginalLine());
				event.setCategories(node.getTitles());

				this.events.add(event);
			}
		}

		for (LineNode child : node.getChildren()) {
			extractEvents(child);
		}
	}

	private int countOccurencesOfCharacter(String text, Character character) {
		int cnt = 0;

		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) != character.charValue())
				break;
			cnt++;
		}

		return cnt;
	}

	public String getEventsOutput() {
		return this.eventsOutput;
	}

	private boolean extractPartialDateFromLine(LineNode node) {
		boolean foundNewDateInformation = false;

		PartialDate date = null;
		if (node.getParent() == null) {
			date = node.getPartialDate();
		} else {
			date = node.getParent().getPartialDate().copy();
		}
		node.setPartialDate(date);

		String line = node.getLine();
		line = line.trim();

		Integer month = getMonth(node.getLine());
		if (month != null) {
			date.addMonth(month.intValue());
			node.setType(LineNode.NodeType.DATE);
			foundNewDateInformation = true;
			return foundNewDateInformation;
		}

		line = removeDateLinks(line);

		// remove leading weekdays
		line = line.replaceAll("^" + regexWeekdays, "");

		line = cleanupText(line);

		node.setOriginalLine(line);
		node.setLine(extractRawText(line));

		String textPart = line;

		System.out.println(line);

		for (DatePattern datePattern : this.datePatterns) {
			// System.out.println(datePattern.getPattern());
			MatcherResult mRes = match(datePattern.getPattern(), line);
			if (mRes.getDatePart() != null) {
				System.out.println("MATCH");
				if (this.year != null && (datePattern.hasM1() || !datePattern.hasM2()))
					date.resetMonths();

				if (datePattern.hasD1())
					date.addDay(Integer.valueOf(mRes.getMatcher().group("d1")));
				if (datePattern.hasD2())
					date.addDay(Integer.valueOf(mRes.getMatcher().group("d2")));

				if (datePattern.hasM1())
					date.addMonth(getMonth(mRes.getMatcher().group("m1")).intValue());
				if (datePattern.hasM2())
					date.addMonth(getMonth(mRes.getMatcher().group("m2")).intValue());

				if (datePattern.hasY1()) {
					date.setYear(Integer.valueOf(mRes.getMatcher().group("y1")));
				}

				foundNewDateInformation = true;
				textPart = mRes.getTextPart();

				break;
			}
		}

		if (textPart == null) {
			node.setType(LineNode.NodeType.DATE);
		} else
			textPart = cleanTextPart(textPart);
		if (textPart == null) {
			node.setType(LineNode.NodeType.DATE);
		}

		node.setOriginalLine(textPart);
		node.setLine(extractRawText(textPart));

		return foundNewDateInformation;
	}

	private String cleanTextPart(String textPart) {

		String oldTextPart = null;
		while (oldTextPart == null || textPart != oldTextPart) {
			oldTextPart = textPart;
			for (String hyphen : this.hyphens)
				textPart = StringUtils.stripStart(textPart, hyphen);
			textPart = StringUtils.stripStart(textPart, ":");
			textPart = StringUtils.stripEnd(textPart, ":");
			textPart = StringUtils.stripStart(textPart, ",");
			textPart = StringUtils.stripEnd(textPart, ",");

			textPart = textPart.trim();
		}

		textPart = StringUtils.capitalize(textPart);

		if (textPart.isEmpty()) {
			textPart = null;
		}

		return textPart;
	}

	private MatcherResult match(Pattern pattern, String line) {
		Matcher matcher = pattern.matcher(line);
		String datePart = null;
		String textPart = line;

		if (matcher.find()) {
			datePart = matcher.group(0);
			textPart = textPart.substring(datePart.length(), textPart.length());
			matcher.reset();
			matcher.find();
		}

		return new MatcherResult(datePart, textPart, matcher);
	}

	private String cleanupText(String line) {
		line = line.replaceAll("'''", "");
		line = line.replaceAll("''", "");
		return line.trim();
	}

	private String removeDateLinks(String line) {
		for (Pattern dateLinkResolver : this.dateLinkResolvers) {
			Matcher matcher = dateLinkResolver.matcher(line);

			StringBuffer sb = new StringBuffer();
			while (matcher.find()) {
				String text = matcher.group("r");
				matcher.appendReplacement(sb, text);
			}
			matcher.appendTail(sb);
			line = sb.toString();
		}

		return line;
	}

	private Integer getMonth(String monthName) {

		for (int i = 0; i < this.monthNames.size(); i++) {
			if (this.monthNames.get(i).contains(monthName)) {
				return Integer.valueOf(i + 1);
			}
		}

		return null;
	}

	private String extractRawText(String text) {

		if (text == null)
			return text;

		text = ReferenceAndTemplateRemover.getInstance(language).removeReferences(text);
		text = ReferenceAndTemplateRemover.getInstance(language).removeTemplates(text, this.dateTemplatePatterns);

		return text;
	}

	private String getEventLine(Event event) {
		if (event.getOriginalText().isEmpty()) {
			return null;
		}

		String line = this.pageId + "\t" + this.pageTitle + Config.TAB + this.year + Config.TAB + this.month
				+ Config.TAB + this.day + "\t" + this.dateFormat.format(event.getStartDate()) + "\t"
				+ this.dateFormat.format(event.getEndDate()) + "\t" + event.getRawText().replaceAll("\t", " ") + "\t"
				+ event.getCategories() + "\t" + event.getGranularity() + Config.TAB
				+ StringUtils.join(event.getLinks(), " ");

		return line;

	}

	private class MatcherResult {
		private String datePart;
		private String textPart;
		private Matcher matcher;

		public MatcherResult(String datePart, String textPart, Matcher matcher) {
			this.datePart = datePart;
			this.textPart = textPart;
			this.matcher = matcher;
		}

		public String getDatePart() {
			return this.datePart;
		}

		public String getTextPart() {
			return this.textPart;
		}

		public Matcher getMatcher() {
			return this.matcher;
		}
	}
}
