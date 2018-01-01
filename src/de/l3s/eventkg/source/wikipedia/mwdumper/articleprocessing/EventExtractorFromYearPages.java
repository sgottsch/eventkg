package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.source.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.event.Event;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.event.LineNode;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.event.PartialDate;

public class EventExtractorFromYearPages {
	private String text;
	private int pageId;
	private Integer year;
	private Integer month;
	private Integer day;

	private String pageTitle;
	private String eventsOutput = "";

	private Language language;

	private List<Event> events;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.US);

	private boolean isYearOrDayPage;

	private EventDateExpressions eventDateExpressions;

	private Map<String, String> redirects;

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
		enMap.put(2843484, "1965 in Australia");
		exampleTexts.put(Language.EN, enMap);

		Map<Integer, String> deMap = new HashMap<Integer, String>();
		deMap.put(83, "Al Pacino");
		deMap.put(5909, "1957");
		deMap.put(499753, "2014");
		deMap.put(1531063, "2017");
		deMap.put(6996295, "Musikjahr 1862");
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
		frMap.put(3627989, "2009 en Palestine");
		exampleTexts.put(Language.FR, frMap);

		Map<Integer, String> ptMap = new HashMap<Integer, String>();
		ptMap.put(13541, "Ronald Reagan");
		ptMap.put(24532, "2007");
		ptMap.put(29071, "3 a.C.");
		ptMap.put(28422, "2017");
		ptMap.put(1989, "1897");
		exampleTexts.put(Language.PT, ptMap);

		Language language = Language.EN;
		int id = 2843484;

		// TODO: Do this before
		WikiWords.getInstance().init(language);
		EventDateExpressionsAll.getInstance().init(language);

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
			this.isYearOrDayPage = false;
			return;
		}

		this.isYearOrDayPage = true;

		this.eventDateExpressions = new EventDateExpressions(this.year, this.month);
		this.eventDateExpressions.init();

		this.eventsOutput = "";
	}

	private void identifyDayPage(String title) {

		MatcherResult mRes = match(EventDateExpressionsAll.getInstance().getDayTitle(), title);
		if (mRes.getDatePart() != null) {
			this.day = Integer.valueOf(mRes.getMatcher().group("d1"));
			this.month = EventDateExpressionsAll.getInstance().getMonth(mRes.getMatcher().group("m1")).intValue();
		}

	}

	public boolean isYearOrDayPage() {
		return this.isYearOrDayPage;
	}

	private Integer extractYearFromTitle(String title) {
		// if ((!title.endsWith(" BC")) && (!title.contains(" BC "))) {

		for (Pattern p : EventDateExpressionsAll.getInstance().getYearTitlePatternsBC()) {
			Matcher m = p.matcher(title);
			if (m.matches()) {
				String text = m.group("y");
				if (!text.startsWith("0")) {
					int year = -Integer.parseInt(text);
					return year;
				}
			}
		}
		for (Pattern p : EventDateExpressionsAll.getInstance().getYearTitlePatterns()) {
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

	public void extractEvents() {
		boolean inEvents = false;

		LineNode root = null;
		LineNode currentNode = null;
		LineNode currentSectionNode = null;

		for (String line : this.text.split("\n")) {

			try {

				line = line.trim();
				// String lineText = StringUtils.strip(line, "= ");

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
				// else if (!inEvents && line.startsWith("=") &&
				// line.endsWith("="))
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
											System.err
													.println("Error with page " + this.pageId + ": " + this.pageTitle);
											System.err.println(e.getMessage() + "\n" + e.getStackTrace());
										}
									}
									currentSectionNode = node;
									currentNode = node;
								}
							} else if (line.startsWith("*")) {
								int level = countOccurencesOfCharacter(line, Character.valueOf('*'));
								line = line.substring(countOccurencesOfCharacter(line, Character.valueOf('*')),
										line.length()).trim();
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

			} catch (NullPointerException e) {
				System.err.println("Error 1 with page " + this.pageId + ": " + this.pageTitle);
				System.err.println(e.getMessage() + "\n" + e.getStackTrace());
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

			try {
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

			} catch (NullPointerException e) {
				System.err.println("Error 2 with page " + this.pageId + ": " + this.pageTitle);
				System.err.println(e.getMessage() + "\n" + e.getStackTrace());
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

		Integer month = EventDateExpressionsAll.getInstance().getMonth(node.getLine());
		if (month != null) {
			date.addMonth(month.intValue());
			node.setType(LineNode.NodeType.DATE);
			foundNewDateInformation = true;
			return foundNewDateInformation;
		}

		line = removeDateLinks(line);

		// remove leading weekdays
		line = line.replaceAll("^" + eventDateExpressions.getRegexWeekdays(), "");
		// remove leading "?" for unknown exact dates as in " ? - Conquista do
		// Japão na Coreia."
		line = line.replaceAll("^\\?", "");

		line = cleanupText(line);

		node.setOriginalLine(line);
		node.setLine(extractRawText(line));

		String textPart = line;

		for (DatePattern datePattern : this.eventDateExpressions.getDatePatterns()) {
			// System.out.println(datePattern.getPattern());
			MatcherResult mRes = match(datePattern.getPattern(), line);
			if (mRes.getDatePart() != null) {
				if (this.year != null && (datePattern.hasM1() || !datePattern.hasM2()))
					date.resetMonths();

				if (datePattern.hasD1())
					date.addDay(Integer.valueOf(mRes.getMatcher().group("d1")));
				if (datePattern.hasD2())
					date.addDay(Integer.valueOf(mRes.getMatcher().group("d2")));

				if (datePattern.hasM1())
					date.addMonth(
							EventDateExpressionsAll.getInstance().getMonth(mRes.getMatcher().group("m1")).intValue());
				if (datePattern.hasM2())
					date.addMonth(
							EventDateExpressionsAll.getInstance().getMonth(mRes.getMatcher().group("m2")).intValue());

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
			for (String hyphen : EventDateExpressionsAll.getInstance().getHyphens())
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
		for (Pattern dateLinkResolver : this.eventDateExpressions.getDateLinkResolvers()) {
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

	private String extractRawText(String text) {

		if (text == null)
			return text;

		text = ReferenceAndTemplateRemover.getInstance(language).removeReferences(text);
		text = ReferenceAndTemplateRemover.getInstance(language).removeTemplates(text,
				eventDateExpressions.getDateTemplatePatterns());

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
