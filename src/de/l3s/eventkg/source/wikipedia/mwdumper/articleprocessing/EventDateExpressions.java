package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class EventDateExpressions {

	private List<Pattern> dateLinkResolvers = new ArrayList<Pattern>();

	private List<DatePattern> datePatterns;

	private String regexMonth;

	private Set<Pattern> dateTemplatePatterns;

	private List<String> hyphens;

	private Integer month;

	private Integer year;

	private String regexYearSuffix;

	public EventDateExpressions(Integer year, Integer month) {
		this.year = year;
		this.month = month;
	}

	public void init() {

		String regexDay1 = EventDateExpressionsAll.getInstance().getPlaceHolder("regexDay1");
		String regexYear = EventDateExpressionsAll.getInstance().getPlaceHolder("regexYear");
		String regexMonth1 = EventDateExpressionsAll.getInstance().getPlaceHolder("regexMonth1");

		this.datePatterns = new ArrayList<DatePattern>();

		if (this.month != null)
			this.datePatterns.add(
					new DatePattern(Pattern.compile("^(?<y1>" + regexYear + ")"), false, false, false, false, true));

		this.dateTemplatePatterns = new HashSet<Pattern>();
		Pattern dateTemplatePattern1 = Pattern
				.compile("^\\{\\{date\\|" + regexDay1 + "\\|" + regexMonth1 + "\\|(?<y1>" + regexYear + ")\\}\\}");
		this.dateTemplatePatterns.add(dateTemplatePattern1);
		this.datePatterns.add(new DatePattern(dateTemplatePattern1, true, false, true, false, true));

		// if (language == Language.EN) {
		//
		// String regexDayMonth1 = regexDay1 + " " + regexMonth1;
		// String regexDayMonth2 = regexDay2 + " " + regexMonth2;
		// String regexMonthDay1 = regexMonth1 + " " + regexDay1;
		// String regexMonthDay2 = regexMonth2 + " " + regexDay2;
		//
		// Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
		//
		// Pattern datePatternMonthDayCommaYear = Pattern.compile("^" +
		// regexMonthDay1 + ", ?" + regexYear);
		//
		// Pattern datePatternDayMonthCommaYear = Pattern.compile("^" +
		// regexDayMonth1 + ", ?" + regexYear);
		//
		// Pattern datePatternMonthDay = Pattern.compile("^" + regexMonthDay1);
		//
		// Pattern datePatternMonthHyphenMonth = Pattern
		// .compile("^" + regexMonth1 + " ?" + hyphensOrWithSlash + " ?" +
		// regexMonth2);
		// this.datePatterns.add(new DatePattern(datePatternMonthHyphenMonth,
		// false, false, true, true));
		//
		// Pattern datePatternMonthDayHyphenMonthDay = Pattern
		// .compile("^" + regexMonthDay1 + hyphensOr + regexMonthDay2);
		// this.datePatterns.add(new
		// DatePattern(datePatternMonthDayHyphenMonthDay, true, true, true,
		// true));
		//
		// Pattern datePatternDayMonthHyphenDayMonth = Pattern
		// .compile("^" + regexDayMonth1 + hyphensOr + regexDayMonth2);
		// this.datePatterns.add(new
		// DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true,
		// true));
		//
		// Pattern datePatternDayMonthHyphenDay = Pattern
		// .compile("^" + regexDayMonth1 + " ?" + hyphensOr + " ?" + regexDay2);
		// this.datePatterns.add(new DatePattern(datePatternDayMonthHyphenDay,
		// true, true, true, false));
		//
		// Pattern datePatternMonthDayHyphenDay = Pattern
		// .compile("^" + regexMonthDay1 + " ?" + hyphensOr + " ?" + regexDay2);
		// this.datePatterns.add(new DatePattern(datePatternMonthDayHyphenDay,
		// true, true, true, false));
		//
		// this.datePatterns.add(new DatePattern(datePatternMonthDayCommaYear,
		// true, false, true, false));
		// this.datePatterns.add(new DatePattern(datePatternDayMonthCommaYear,
		// true, false, true, false));
		// this.datePatterns.add(new DatePattern(datePatternDayMonth, true,
		// false, true, false));
		// this.datePatterns.add(new DatePattern(datePatternMonthDay, true,
		// false, true, false));
		//
		// Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
		// this.datePatterns.add(new DatePattern(datePatternMonth, false, false,
		// true, false));
		//
		// Pattern datePatternDay = Pattern.compile("^" + regexDay1);
		// this.datePatterns.add(new DatePattern(datePatternDay, true, false,
		// false, false));
		//
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexMonth1 + " " +
		// regexDay1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " +
		// regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexMonth1 + "
		// " + regexDay1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " "
		// + regexMonth1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear +
		// ")\\]\\]"));
		// } else if (language == Language.DE) {
		//
		// String regexDayMonth1 = regexDay1 + "\\. " + regexMonth1;
		// String regexDayMonth2 = regexDay2 + "\\. " + regexMonth2;
		//
		// String hyphensOrWithSlashAndBis = "(" + StringUtils.join(hyphens,
		// "|") + "|/|(bis)" + ")";
		//
		// Pattern datePatternDayMonthHyphenDayMonth = Pattern
		// .compile("^" + regexDayMonth1 + " ?" + hyphensOrWithSlashAndBis + "
		// ?" + regexDayMonth2);
		// this.datePatterns.add(new
		// DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true,
		// true));
		// Pattern datePatternDayHyphenDayMonth = Pattern
		// .compile("^" + regexDay1 + "\\. ?" + hyphensOrWithSlashAndBis + " ?"
		// + regexDayMonth2);
		// this.datePatterns.add(new DatePattern(datePatternDayHyphenDayMonth,
		// true, true, false, true));
		// Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1 +
		// "( " + year + ")?");
		// this.datePatterns.add(new DatePattern(datePatternDayMonth, true,
		// false, true, false));
		// Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
		// this.datePatterns.add(new DatePattern(datePatternMonth, false, false,
		// true, false));
		// Pattern datePatternDayFrom = Pattern.compile("^(Ab |ab )" +
		// regexDayMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayFrom, true,
		// false, true, false));
		// Pattern datePatternDayUntil = Pattern.compile("^(Bis |bis )" +
		// regexDayMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayUntil, true,
		// false, true, false));
		//
		// dateLinkResolvers
		// .add(Pattern.compile("\\[\\[" + regexDay1 + "\\. " + regexMonth1 +
		// "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 +
		// "\\. " + regexMonth1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear +
		// ")\\]\\]"));
		// } else if (language == Language.FR) {
		//
		// String regexDayMonth1 = regexDay1 + " " + regexMonth1;
		//
		// String hyphensOrWithSlashAndText = "(" + StringUtils.join(hyphens,
		// "|") + "|/|(et|au)" + ")";
		//
		// Pattern datePatternDayMonthDayMonth = Pattern.compile("^" + regexDay1
		// + " " + regexMonth1 + " ?"
		// + hyphensOrWithSlashAndText + " ?" + regexDay2 + " " + regexMonth2);
		// this.datePatterns.add(new DatePattern(datePatternDayMonthDayMonth,
		// true, true, true, true));
		// Pattern datePatternDayDayMonth = Pattern
		// .compile("^" + regexDay1 + " ?" + hyphensOrWithSlashAndText + " ?" +
		// regexDay2 + " " + regexMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayDayMonth, true,
		// true, true, false));
		// Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayMonth, true,
		// false, true, false));
		// Pattern datePatternMonth = Pattern.compile("^" + regexMonth1);
		// this.datePatterns.add(new DatePattern(datePatternMonth, false, false,
		// true, false));
		//
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " +
		// regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " "
		// + regexMonth1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexMonth1 + " " +
		// year + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear +
		// ")\\]\\]"));
		// } else if (language == Language.PT) {
		//
		// String regexDayMonth1 = "(Em )?" + regexDay1 + "º? de " +
		// regexMonth1;
		// Pattern datePatternDayDayMonth = Pattern
		// .compile("^" + regexDay1 + " (a|e) " + regexDay2 + " de " +
		// regexMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayDayMonth, true,
		// true, true, false));
		// Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayMonth, true,
		// false, true, false));
		//
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDayMonth1 +
		// "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDayMonth1
		// + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear +
		// ")\\]\\]"));
		// } else if (language == Language.RU) {
		//
		// String regexDayMonth1 = regexDay1 + " " + regexMonth1;
		// String regexDayMonth2 = regexDay2 + " " + regexMonth2;
		//
		// Pattern datePatternDayMonthHyphenDayMonth = Pattern
		// .compile("^" + regexDayMonth1 + " ?" + hyphensOrWithSlash + " ?" +
		// regexDayMonth2);
		// this.datePatterns.add(new
		// DatePattern(datePatternDayMonthHyphenDayMonth, true, true, true,
		// true));
		// Pattern datePatternDayHyphenDayMonth = Pattern
		// .compile("^" + regexDay1 + " ?" + hyphensOrWithSlash + " ?" +
		// regexDayMonth2);
		// this.datePatterns.add(new DatePattern(datePatternDayHyphenDayMonth,
		// true, true, false, true));
		// Pattern datePatternDayMonth = Pattern.compile("^" + regexDayMonth1);
		// this.datePatterns.add(new DatePattern(datePatternDayMonth, true,
		// false, true, false));
		//
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " +
		// regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " "
		// + regexMonth1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear + ")(
		// (год))?\\]\\]"));
		// }

		for (String datePatternLine : EventDateExpressionsAll.getInstance().getEntriesPerType("datePatterns")) {
			String[] parts = datePatternLine.split("\t");

			String datePattern = parts[0];
			boolean hasStartDay = parts[1].equals("1");
			boolean hasEndDay = parts[2].equals("1");
			boolean hasStartMonth = parts[3].equals("1");
			boolean hasEndMonth = parts[4].equals("1");
			boolean hasYear = false;
			if (parts.length > 5 && parts[5].equals("1"))
				hasYear = true;

			datePattern = datePattern.replaceAll("@thisYear@", String.valueOf(this.year));
			datePattern = datePattern.replaceAll("@thisMonth@", String.valueOf(this.month));

			Pattern pattern = Pattern.compile(EventDateExpressionsAll.getInstance().createPatternString(datePattern));
			this.datePatterns
					.add(new DatePattern(pattern, hasStartDay, hasEndDay, hasStartMonth, hasEndMonth, hasYear));
		}

		for (String datePatternLine : EventDateExpressionsAll.getInstance().getEntriesPerType("dateLinkResolvers")) {
			datePatternLine = datePatternLine.replaceAll("@thisYear@", String.valueOf(this.year));
			dateLinkResolvers
					.add(Pattern.compile(EventDateExpressionsAll.getInstance().createPatternString(datePatternLine)));
		}

		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexMonth1 + " " +
		// regexDay1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[" + regexDay1 + " " +
		// regexMonth1 + "\\|(?<r>[^\\]]*)\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexMonth1 + "
		// " + regexDay1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexDay1 + " "
		// + regexMonth1 + ")\\]\\]"));
		// dateLinkResolvers.add(Pattern.compile("\\[\\[(?<r>" + regexYear +
		// ")\\]\\]"));

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

	public Set<Pattern> getDateTemplatePatterns() {
		return dateTemplatePatterns;
	}

	public List<String> getHyphens() {
		return hyphens;
	}

	public String getRegexYearSuffix() {
		return regexYearSuffix;
	}

}
