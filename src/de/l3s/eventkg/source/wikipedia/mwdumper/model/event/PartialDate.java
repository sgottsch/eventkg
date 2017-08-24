package de.l3s.eventkg.source.wikipedia.mwdumper.model.event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class PartialDate {

	public static void main(String[] args) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("G yyyy-MM-dd");

		PartialDate date = new PartialDate();
		date.setYear(-200);
		System.out.println(date.transformToStartDate());
		System.out.println(date.transformToEndDate());

		System.out.println(dateFormat.format(date.transformToStartDate()));
		System.out.println(dateFormat.format(date.transformToEndDate()));

	}

	private Integer year;
	private List<Integer> months;
	private List<Integer> days;

	public PartialDate() {
		this.months = new ArrayList<Integer>();
		this.days = new ArrayList<Integer>();
	}

	public Integer getYear() {
		return this.year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public List<Integer> getMonths() {
		return this.months;
	}

	public List<Integer> getDays() {
		return this.days;
	}

	public void addDay(int day) {
		if (!this.days.contains(day)) {
			this.days.add(day);
		}
	}

	public void addMonth(int month) {
		if (!this.months.contains(month)) {
			this.months.add(month);
		}
		if (this.months.size() == 12) {
			this.months = new ArrayList<Integer>();
		}
	}

	public String toString() {
		String output = "";
		if (this.year != null) {
			output = output + this.year;
		} else {
			output = output + "-";
		}
		output = output + "|";
		if (!this.months.isEmpty()) {
			output = output + "{" + StringUtils.join(this.months, ",") + "}";
		} else {
			output = output + "-";
		}
		output = output + "|";
		if (!this.days.isEmpty()) {
			output = output + "{" + StringUtils.join(this.days, ",") + "}";
		} else {
			output = output + "-";
		}
		return output;
	}

	public PartialDate copy() {
		PartialDate date = new PartialDate();
		date.setYear(this.year);
		for (int month : months)
			date.addMonth(month);
		for (int day : days)
			date.addDay(day);
		return date;
	}

	public Date transformToStartDate() {
		Integer month = null;
		if (this.months.isEmpty()) {
			month = Integer.valueOf(1);
		} else {
			month = (Integer) Collections.min(this.months);
		}
		Integer day = null;
		if (this.days.isEmpty()) {
			day = Integer.valueOf(1);
		} else {
			day = (Integer) Collections.min(this.days);
		}
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		month = Integer.valueOf(month.intValue() - 1);
		calendar.set(1, this.year.intValue());
		calendar.set(2, month.intValue());
		calendar.set(5, day.intValue());
		Date date = calendar.getTime();

		return date;
	}

	public Date transformToEndDate() {
		Integer month = null;
		if (this.months.isEmpty()) {
			month = Integer.valueOf(12);
		} else {
			month = (Integer) Collections.max(this.months);
		}
		month = Integer.valueOf(month.intValue() - 1);
		Integer day = null;
		if (!this.days.isEmpty()) {
			day = (Integer) Collections.max(this.days);
		}
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(1, this.year.intValue());
		calendar.set(2, month.intValue());
		if (day == null) {
			calendar.set(5, calendar.getActualMaximum(5));
		} else {
			calendar.set(5, day.intValue());
		}
		Date date = calendar.getTime();

		return date;
	}

	public Granularity getGranularity() {
		if (!this.days.isEmpty()) {
			return Granularity.DAY;
		}
		if (!this.months.isEmpty()) {
			return Granularity.MONTH;
		}
		return Granularity.YEAR;
	}

	public static enum Granularity {
		YEAR,
		MONTH,
		DAY;
	}

	public void resetMonths() {
		this.months = new ArrayList<Integer>();
	}
}
