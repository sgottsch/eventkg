package de.l3s.eventkg.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeTransformer {

	private static SimpleDateFormat xsdDateFormat = new SimpleDateFormat("G yyyy-MM-dd");
	// private static SimpleDateFormat wikidataDateFormat = new
	// SimpleDateFormat("yyyyyyyyyyy-MM-dd'T'hh:mm:ss");
	private static SimpleDateFormat wikidataDateFormat = new SimpleDateFormat("G yyyyyyyyyyy-MM-dd");

	private static SimpleDateFormat dbPediaDateFormat = new SimpleDateFormat("G yyyy-MM-dd");

	public static void main(String[] args) throws ParseException {
		// System.out.println(new Date(Long.MAX_VALUE).toString());
		// System.out.println(generateEarliestTimeForWikidata("00000001969-01-01T00:00:00"));
		// System.out.println(generateEarliestTimeForWikidata("+00000001969-00-00T00:00:00Z"));
		System.out.println(generateTimeForDBpedia("\"1618-05-23\"^^<http://www.w3.org/2001/XMLSchema#date>"));
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

	public static Date generateEarliestTimeForWikidata(String timeString) throws ParseException {

		// ignore empty dates
		if (timeString.startsWith("00000000000"))
			return null;

		int era = 1;
		if (timeString.startsWith("-"))
			era = -1;
		timeString = timeString.substring(1);

		// remove time, only keep date
		timeString = timeString.substring(0, timeString.indexOf("T"));

		if (era == 1)
			timeString = timeString.replaceAll("-00-", "-01-");
		else
			timeString = timeString.replaceAll("-00-", "-12-");

		if (era == 1)
			timeString = timeString.replaceAll("-00", "-01");
		else {
			long year = Long.valueOf(timeString.substring(0, timeString.indexOf("-")));
			int month = Integer.valueOf(timeString.substring(timeString.indexOf("-"), timeString.lastIndexOf("-")));
			timeString = timeString.replaceAll("-00", "-" + String.valueOf(getLastDayInMonth(year, month, era)));
		}

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return wikidataDateFormat.parse(timeString);
	}

	public static Date generateLatestTimeForWikidata(String timeString) throws ParseException {

		// ignore empty dates
		if (timeString.startsWith("00000000000"))
			return null;

		int era = 1;
		if (timeString.startsWith("-"))
			era = -1;
		timeString = timeString.substring(1);

		// remove time, only keep date
		timeString = timeString.substring(0, timeString.indexOf("T"));

		if (era == 1)
			timeString = timeString.replaceAll("-00-", "-12-");
		else
			timeString = timeString.replaceAll("-00-", "-01-");

		if (era == 1) {
			long year = Long.valueOf(timeString.substring(0, timeString.indexOf("-")));
			int month = Integer.valueOf(timeString.substring(timeString.indexOf("-"), timeString.lastIndexOf("-")));
			timeString = timeString.replaceAll("-00", "-" + String.valueOf(getLastDayInMonth(year, month, era)));
		} else {
			timeString = timeString.replaceAll("-00", "-01");
		}

		if (era == -1)
			timeString = "BC " + timeString;
		else
			timeString = "AD " + timeString;

		return wikidataDateFormat.parse(timeString);
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

		if (era == 1)
			timeString = timeString.replaceAll("-##-", "-01-");
		else
			timeString = timeString.replaceAll("-##-", "-12-");

		if (era == 1)
			timeString = timeString.replaceAll("-##", "-01");
		else {
			int year = Integer.valueOf(timeString.substring(0, timeString.indexOf("-")));
			int month = Integer.valueOf(timeString.substring(5, 7));
			timeString = timeString.replaceAll("-##", "-" + String.valueOf(getLastDayInMonth(year, month, era)));
		}

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

		if (era == 1)
			timeString = timeString.replaceAll("-##-", "-12-");
		else
			timeString = timeString.replaceAll("-##-", "-01-");

		if (era == 1) {
			int year = Integer.valueOf(timeString.substring(0, timeString.indexOf("-")));
			int month = Integer.valueOf(timeString.substring(5, 7));
			timeString = timeString.replaceAll("-##", "-" + String.valueOf(getLastDayInMonth(year, month, era)));
		} else
			timeString = timeString.replaceAll("-##", "-01");

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
