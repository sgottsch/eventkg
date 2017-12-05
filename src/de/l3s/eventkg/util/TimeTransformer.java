package de.l3s.eventkg.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class TimeTransformer {

	private static SimpleDateFormat xsdDateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);
	// private static SimpleDateFormat wikidataDateFormat = new
	// SimpleDateFormat("yyyyyyyyyyy-MM-dd'T'hh:mm:ss");
	private static SimpleDateFormat wikidataDateFormat = new SimpleDateFormat("G yyyyyyyyyyy-MM-dd", Locale.ENGLISH);

	private static SimpleDateFormat dbPediaDateFormat = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);

	public static void main(String[] args) throws ParseException {

		// System.out.println(new Date(Long.MAX_VALUE).toString());
		// System.out.println(generateEarliestTimeForWikidata("00000001969-01-01T00:00:00"));
		// System.out.println(generateEarliestTimeForWikidata("+00000001969-00-00T00:00:00Z"));
		// System.out.println(generateTimeForDBpedia("\"1618-05-23\"^^<http://www.w3.org/2001/XMLSchema#date>"));

		// +00000002003-06-01T00:00:00Z

		// System.out.println(generateEarliestTimeForWikidata("10;+00000002003-06-01T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("10;+00000002003-06-01T00:00:00Z"));
		//
		// System.out.println(generateEarliestTimeForWikidata("9;+00000002003-01-01T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("9;+00000002003-01-01T00:00:00Z"));
		//
		// System.out.println(generateEarliestTimeForWikidata("8;+00000001980-01-01T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("8;+00000001900-01-01T00:00:00Z"));

		// System.out.println(generateLatestTimeForWikidata("7;-00000002500-00-00T00:00:00Z"));

		System.out.println(generateEarliestTimeForWikidata("8;+00000001940-01-01T00:00:00Z"));
		System.out.println(generateLatestTimeForWikidata("8;+00000001940-01-01T00:00:00Z"));
		System.out.println("---");
		System.out.println(generateEarliestTimeForWikidata("8;-00000001940-01-01T00:00:00Z"));
		System.out.println(generateLatestTimeForWikidata("8;-00000001940-01-01T00:00:00Z"));

		// System.out.println(generateEarliestTimeForWikidata("7;-00000001178-00-00T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("7;-00000001178-00-00T00:00:00Z"));
		// System.out.println(generateEarliestTimeForWikidata("8;+00000001980-01-01T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("8;+00000001980-01-01T00:00:00Z"));
		// System.out.println(generateEarliestTimeForWikidata("3;-13798000000-00-00T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("3;-13798000000-00-00T00:00:00Z"));

		// System.out.println(generateEarliestTimeForWikidata("11;+00000002006-08-15T00:00:00Z"));
		// System.out.println(generateLatestTimeForWikidata("9;+00000002006-00-00T00:00:00Z"));
		// System.out.println(dbPediaDateFormat.format(generateEarliestTimeForWikidata("11;-00000000300-00-00T00:00:00Z")));
		// System.out.println(dbPediaDateFormat.format(generateEarliestTimeForWikidata("11;+00000000300-00-00T00:00:00Z")));
		// System.out.println(dbPediaDateFormat.format(generateLatestTimeForWikidata("11;-00000000300-00-00T00:00:00Z")));
		// System.out.println(dbPediaDateFormat.format(generateLatestTimeForWikidata("11;+00000000300-00-00T00:00:00Z")));
		// System.out.println(dbPediaDateFormat.format(generateLatestTimeForWikidata("11;-00000002007-11-00T00:00:00Z")));
		// System.out.println(dbPediaDateFormat.format(generateLatestTimeForWikidata("11;+00000002007-11-00T00:00:00Z")));
		//
		// System.out.println(dbPediaDateFormat
		// .format(generateEarliestTimeFromXsd("\"1618-##-##\"^^<http://www.w3.org/2001/XMLSchema#date>")));

	}

	public static Date generateTimeForDBpedia(String timeString) throws ParseException {
		// e.g. "1618-05-23"^^<http://www.w3.org/2001/XMLSchema#date>

		timeString = timeString.substring(1);
		timeString = timeString.substring(0, timeString.indexOf("\""));

		boolean bc = false;
		if (timeString.startsWith("-")) {
			bc = true;
			timeString = timeString.substring(1);
		}

		if (bc)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return dbPediaDateFormat.parse(timeString);
	}

	public static Date generateEarliestTimeForWikidata(String timeAndPrecisionString) throws ParseException {

		// in the case of BC, we need to swap earliest and latest.
		// Example
		// the decade of "1940"
		// a) 1940 AD
		// Jan 1, 1940 - Dec 31, 1949
		// b) 1940 BC
		// Jan 1, 1949 - Dec 31, 1940

		String timeString = timeAndPrecisionString.split(";")[1];

		int era = 1;
		if (timeString.startsWith("-"))
			era = -1;
		timeString = timeString.substring(1);

		if (era == 1) {
			return generateEarliestTimeForWikidata(timeAndPrecisionString, 1);
		} else {
			Date date1 = generateEarliestTimeForWikidata(timeAndPrecisionString, -1);
			if (date1 == null)
				return null;
			Calendar calendar1 = new GregorianCalendar();
			calendar1.setTime(date1);
			int day = calendar1.get(Calendar.DAY_OF_MONTH);
			int month = calendar1.get(Calendar.MONTH) + 1;

			Date date2 = generateLatestTimeForWikidata(timeAndPrecisionString, -1);
			if (date2 == null)
				return null;
			Calendar calendar2 = new GregorianCalendar();
			calendar2.setTime(date2);
			int year = calendar2.get(Calendar.YEAR);

			// day and month from earliest. Year from latest.
			return wikidataDateFormat.parse("BC " + year + "-" + month + "-" + day);
		}
	}

	public static Date generateEarliestTimeForWikidata(String timeAndPrecisionString, int era) throws ParseException {

		String timeString = timeAndPrecisionString.split(";")[1];
		timeString = timeString.substring(1);
		int precision = Integer.valueOf(timeAndPrecisionString.split(";")[0]);

		// we do not need to care about the precision. Because the beginning is
		// always the given date. It does not matter whether it happened on the
		// first day or it is just the notation for a whole month (which is
		// also the first day in the start time)

		// ignore empty dates
		if (precision < 6 || timeString.startsWith("00000000000"))
			return null;

		// remove time, only keep date
		timeString = timeString.substring(0, timeString.indexOf("T"));

		timeString = timeString.replaceAll("-00-", "-01-");

		timeString = timeString.replaceAll("-00", "-01");

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return wikidataDateFormat.parse(timeString);
	}

	public static Date generateLatestTimeForWikidata(String timeAndPrecisionString) throws ParseException {

		// in the case of BC, we need to swap earliest and latest.
		// Example
		// the decade of "1940"
		// a) 1940 AD
		// Jan 1, 1940 - Dec 31, 1949
		// b) 1940 BC
		// Jan 1, 1949 - Dec 31, 1940

		String timeString = timeAndPrecisionString.split(";")[1];

		int era = 1;
		if (timeString.startsWith("-"))
			era = -1;
		timeString = timeString.substring(1);

		if (era == 1) {
			return generateLatestTimeForWikidata(timeAndPrecisionString, 1);
		} else {
			Date date1 = generateLatestTimeForWikidata(timeAndPrecisionString, -1);
			if (date1 == null)
				return null;
			Calendar calendar1 = new GregorianCalendar();
			calendar1.setTime(date1);
			int day = calendar1.get(Calendar.DAY_OF_MONTH);
			int month = calendar1.get(Calendar.MONTH) + 1;

			Date date2 = generateEarliestTimeForWikidata(timeAndPrecisionString, -1);
			if (date2 == null)
				return null;
			Calendar calendar2 = new GregorianCalendar();
			calendar2.setTime(date2);
			int year = calendar2.get(Calendar.YEAR);

			// day and month from earliest. Year from latest.
			return wikidataDateFormat.parse("BC " + year + "-" + month + "-" + day);
		}

	}

	public static Date generateLatestTimeForWikidata(String timeAndPrecisionString, int era) throws ParseException {

		// System.out.println("\n---\ngenerateLatestTimeForWikidata: " +
		// timeAndPrecisionString + "\n");

		String timeString = timeAndPrecisionString.split(";")[1];
		timeString = timeString.substring(1);
		int precision = Integer.valueOf(timeAndPrecisionString.split(";")[0]);

		// precision values
		// 0: billion years
		// 1: hundred million years
		// 2-5: ...
		// 6: millenia
		// 7: century
		// 8: decade
		// 9: year
		// 10: month
		// 11: day
		// 12: hour
		// 13: minute
		// 14: seconds

		// ignore empty dates
		if (precision < 6 || timeString.startsWith("00000000000"))
			return null;

		// remove time, only keep date
		timeString = timeString.substring(0, timeString.indexOf("T"));
		// replace "00" month with december
		timeString = timeString.replaceAll("-00-", "-12-");
		if (precision < 10)
			timeString = timeString.replaceAll("-01-", "-12-");

		long year = Long.valueOf(timeString.substring(0, timeString.indexOf("-")));
		int month = Integer.valueOf(timeString.substring(timeString.indexOf("-") + 1, timeString.lastIndexOf("-")));
		// if no month is given: take the last possible one

		// ripple through the year. Start at the right. If precision is bad,
		// replace last number with a 9
		int placeFromRight = 1;
		boolean changed = true;
		int currentPrecision = 8;

		while (changed) {
			changed = false;
			if (getNthLastDigit(year, placeFromRight) == 0 && precision <= currentPrecision) {
				year = year + (long) (Math.pow(10, placeFromRight - 1) * 9);
				changed = true;
			}
			currentPrecision -= 1;
			placeFromRight += 1;
		}

		// day
		timeString = timeString.replaceAll("-00$", "-" + String.valueOf(getLastDayInMonth(year, month, era)));
		if (precision < 11)
			timeString = timeString.replaceAll("-01$", "-" + String.valueOf(getLastDayInMonth(year, month, era)));

		// replace year of time string with new year
		timeString = year + "-" + timeString.substring(timeString.indexOf("-") + 1);

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return wikidataDateFormat.parse(timeString);
	}

	private static long getNthLastDigit(long number, int n) {
		return (long) (Math.abs(number) / Math.pow(10, n - 1)) % 10;
	}

	public static Date generateEarliestTimeFromXsd(String timeString) throws ParseException {

		// System.out.println(timeString);
		timeString = cleanupXsdTimeString(timeString);
		// System.out.println("\t" + timeString);

		int era = 1;
		if (timeString.startsWith("-")) {
			// BC times have a leading "-". Remember that and remove it from
			// string
			era = -1;
			timeString = timeString.substring(1);
		}

		if (timeString.startsWith("#")) {
			// Ignore times without years
			return null;
		}

		// if year has less than four digits -> add leading zeroes
		while (timeString.indexOf("-") < 4)
			timeString = "0" + timeString;

		// for cases where the year is like "19##"
		if (timeString.substring(2, 4).equals("##")) {
			if (era == 1)
				timeString = timeString.substring(0, 2) + "00" + timeString.substring(4);
			else
				timeString = timeString.substring(0, 2) + "99" + timeString.substring(4);
		}

		timeString = timeString.replaceAll("-##-", "-01-");

		timeString = timeString.replaceAll("-##", "-01");

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return xsdDateFormat.parse(timeString);
	}

	public static Date generateLatestTimeFromXsd(String timeString) throws ParseException {

		// System.out.println(timeString);
		timeString = cleanupXsdTimeString(timeString);

		// System.out.println("\t" + timeString);

		int era = 1;
		if (timeString.startsWith("-")) {
			// BC times have a leading "-". Remember that and remove it from
			// string
			era = -1;
			timeString = timeString.substring(1);
		}

		if (timeString.startsWith("#")) {
			// Ignore times without years
			return null;
		}

		// if year has less than four digits -> add leading zeroes
		while (timeString.indexOf("-") < 4)
			timeString = "0" + timeString;

		// for cases where the year is like "19##"
		if (timeString.substring(2, 4).equals("##")) {
			if (era == 1)
				timeString = timeString.substring(0, 2) + "99" + timeString.substring(4);
			else
				timeString = timeString.substring(0, 2) + "00" + timeString.substring(4);
		}

		timeString = timeString.replaceAll("-##-", "-12-");

		int year = Integer.valueOf(timeString.substring(0, timeString.indexOf("-")));
		int month = Integer.valueOf(timeString.substring(5, 7));
		timeString = timeString.replaceAll("-##", "-" + String.valueOf(getLastDayInMonth(year, month, era)));

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return xsdDateFormat.parse(timeString);
	}

	private static int getLastDayInMonth(long year, int month, int era) {

		// no leap year checking in extreme case
		if (year > Integer.MAX_VALUE) {
			if (month == 2) {
				return 28;
			} else if (month == 2 || month == 4 || month == 6 || month == 9 || month == 11) {
				return 30;
			} else
				return 31;
		}

		if (month == 2) {
			// special case: February with leap years
			Calendar mycal = new GregorianCalendar((int) year * era, month, 1);
			int daysInMonth = mycal.getActualMaximum(Calendar.DAY_OF_MONTH); // 28
			return daysInMonth;
		} else if (month == 2 || month == 4 || month == 6 || month == 9 || month == 11) {
			return 30;
		} else
			return 31;
	}

	private static String cleanupXsdTimeString(String timeString) {
		// transform e.g. '"1989-##-##"^^xsd:date .' into '1989-##-##'
		if (timeString.startsWith("\""))
			timeString = timeString.substring(1, timeString.lastIndexOf("\""));
		return timeString;
	}

}
