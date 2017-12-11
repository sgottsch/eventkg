package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.source.wikipedia.WikiWords;

public class EventDateExpressions2BU {

	private Language language;

	private List<Pattern> dateLinkResolvers = new ArrayList<Pattern>();

	private List<DatePattern> datePatterns;

	private String regexMonth;
	private String regexWeekdays;

	private Set<Pattern> dateTemplatePatterns;

	private List<String> hyphens;

	private Integer month;

	private Integer year;

	public EventDateExpressions2BU(Language language) {
		this.language = language;
	}

	public void init() {

		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		this.regexWeekdays = WikiWords.getInstance().getWeekdayRegex(language);
		String regexMonth1 = "(?<m1>" + this.regexMonth.substring(1);

		String regexDay1 = "(?<d1>[1-3]?[0-9])";

		// en: January 22
		// de: 22. Januar
		// fr: 22 janvier
		// pt: 22 de janeiro
		// ru: 22 января

		// ---

		this.regexMonth = WikiWords.getInstance().getMonthRegex(language);
		String regexMonth2 = "(?<m2>" + this.regexMonth.substring(1);

		this.hyphens = new ArrayList<String>();
		hyphens.add("-");
		hyphens.add("–");
		hyphens.add("—");
		hyphens.add("-");
		hyphens.add("—");

		String hyphensOr = ("(" + StringUtils.join(hyphens, "|") + ")");
		String hyphensOrWithSlash = "(" + StringUtils.join(hyphens, "|") + "|/" + ")";

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
					.compile("^" + regexMonthDay1 + hyphensOr + regexMonthDay2);
			this.datePatterns.add(new DatePattern(datePatternMonthDayHyphenMonthDay, true, true, true, true));

			Pattern datePatternDayMonthHyphenDayMonth = Pattern
					.compile("^" + regexDayMonth1 + hyphensOr + regexDayMonth2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true, true));

			Pattern datePatternDayMonthHyphenDay = Pattern
					.compile("^" + regexDayMonth1 + " ?" + hyphensOr + " ?" + regexDay2);
			this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDay, true, true, true, false));

			Pattern datePatternMonthDayHyphenDay = Pattern
					.compile("^" + regexMonthDay1 + " ?" + hyphensOr + " ?" + regexDay2);
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

			String hyphensOrWithSlashAndBis = "(" + StringUtils.join(hyphens, "|") + "|/|(bis)" + ")";

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

			String hyphensOrWithSlashAndText = "(" + StringUtils.join(hyphens, "|") + "|/|(et|au)" + ")";

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

			String regexDayMonth1 = "(Em )?" + regexDay1 + "º? de " + regexMonth1;
			Pattern datePatternDayDayMonth = Pattern
					.compile("^" + regexDay1 + " (a|e) " + regexDay2 + " de " + regexMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayDayMonth, true, true, true, false));
			Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
			this.datePatterns.add(new DatePattern(datePatternDayMonth, true, false, true, false));

			dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDayMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
			dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDayMonth1 + ")\\]\\]"));
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

	public List<Pattern> getDateLinkResolvers() {
		return dateLinkResolvers;
	}

	public List<DatePattern> getDatePatterns() {
		return datePatterns;
	}

	public String getRegexMonth() {
		return regexMonth;
	}

	public String getRegexWeekdays() {
		return regexWeekdays;
	}

	public Set<Pattern> getDateTemplatePatterns() {
		return dateTemplatePatterns;
	}

	public List<String> getHyphens() {
		return hyphens;
	}

}
