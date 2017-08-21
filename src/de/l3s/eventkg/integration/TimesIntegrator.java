package de.l3s.eventkg.integration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import edu.stanford.nlp.util.StringUtils;

public class TimesIntegrator extends Extractor {

	private SimpleDateFormat sdf = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();

	public TimesIntegrator(List<Language> languages) {
		super("TimesIntegrator", Source.ALL, "Integrate start and end times into a common graph.", languages);
	}

	private enum DateType {
		START,
		END;
	}

	public void run() {
		System.out.println("Integrate times by earliest and latest time.");
		integrateTimesByTime();
		System.out.println("Integrate times by majority/trust.");
		integrateTimesByTrust();
	}

	private void integrateTimesByTime() {

		for (Entity entity : DataStore.getInstance().getEntities()) {

			// take the earliest start time
			if (entity.getStartTimesWithDataSets() != null && !entity.getStartTimesWithDataSets().isEmpty()) {
				Date startTime = earliestStartTime(entity.getStartTimesWithDataSets());
				if (startTime == null)
					System.out.println("Start time null: integrateTimesByTime - " + entity.getWikidataId());
				DataStore.getInstance().addStartTime(new StartTime(entity,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED_TIME_2), startTime));
			}

			// take the latest end time
			if (entity.getEndTimesWithDataSets() != null && !entity.getEndTimesWithDataSets().isEmpty()) {
				Date endTime = latestEndTime(entity.getEndTimesWithDataSets());
				if (endTime == null)
					System.out.println("End time null: integrateTimesByTime" + entity.getWikidataId());
				DataStore.getInstance().addEndTime(new EndTime(entity,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED_TIME_2), endTime));
			}

		}

	}

	private Date earliestStartTime(Map<Date, Set<DataSet>> startTimesWithDataSets) {
		Date earliestTime = null;

		for (Date date : startTimesWithDataSets.keySet()) {
			if (earliestTime == null || date.before(earliestTime))
				earliestTime = date;
		}

		return earliestTime;
	}

	private Date latestEndTime(Map<Date, Set<DataSet>> endTimesWithDataSets) {
		Date latestTime = null;

		for (Date date : endTimesWithDataSets.keySet()) {
			if (date == null)
				System.out.println("input end date is null");
			if (latestTime == null || date.after(latestTime))
				latestTime = date;
		}

		return latestTime;
	}

	private void integrateTimesByTrust() {

		boolean print = false;

		initDataSetsByTrustWorthiness();

		int i = 0;
		for (Event event : DataStore.getInstance().getEvents()) {

			i += 1;

			if ((event.getStartTimesWithDataSets().isEmpty() || event.getStartTimesWithDataSets() == null)
					&& (event.getEndTimesWithDataSets().isEmpty() || event.getEndTimesWithDataSets() == null))
				continue;

			if (print) {
				System.out.println("");
				System.out.println(i + "/" + DataStore.getInstance().getEvents().size() + ": " + event.getWikidataId()
						+ " / " + event.getWikipediaLabel(Language.EN));
			}

			if (print) {
				List<String> beforeStartDates = new ArrayList<String>();
				for (Date d : event.getStartTimesWithDataSets().keySet()) {
					List<String> dataSets = new ArrayList<String>();
					for (DataSet ds : event.getStartTimesWithDataSets().get(d))
						dataSets.add(ds.getId());
					beforeStartDates.add(sdf.format(d) + "/" + StringUtils.join(dataSets, " "));
				}
				System.out.println("Before, start: " + StringUtils.join(beforeStartDates, " "));
			}

			Date startTime = null;
			if (event.getStartTimesWithDataSets() != null && !event.getStartTimesWithDataSets().isEmpty()) {
				startTime = integrateTimesOfEvent(event.getStartTimesWithDataSets(), DateType.START, null);
				if (startTime != null)
					DataStore.getInstance().addStartTime(new StartTime(event,
							DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED_TIME), startTime));
			}

			if (print && startTime != null)
				System.out.println("After, start: " + sdf.format(startTime));

			List<DataSet> dataSetsByTrustWorthinessCopy2 = new ArrayList<DataSet>();
			dataSetsByTrustWorthinessCopy2.addAll(dataSetsByTrustWorthiness);

			Date endTime = null;
			Map<Date, Set<DataSet>> endTimesWithDataSetsDeepCopy = new HashMap<Date, Set<DataSet>>();
			if (event.getEndTimesWithDataSets() != null) {
				for (Date date : event.getEndTimesWithDataSets().keySet()) {
					endTimesWithDataSetsDeepCopy.put(date, new HashSet<DataSet>());
					for (DataSet dataSet : event.getEndTimesWithDataSets().get(date)) {
						endTimesWithDataSetsDeepCopy.get(date).add(dataSet);
					}

				}
			}

			if (print) {
				List<String> beforeEndDates = new ArrayList<String>();
				for (Date d : event.getEndTimesWithDataSets().keySet()) {
					List<String> dataSets = new ArrayList<String>();
					for (DataSet ds : event.getEndTimesWithDataSets().get(d))
						dataSets.add(ds.getId());
					beforeEndDates.add(sdf.format(d) + "/" + StringUtils.join(dataSets, " "));
				}
				System.out.println("Before, end: " + StringUtils.join(beforeEndDates, " "));
			}

			while (true) {
				if (!endTimesWithDataSetsDeepCopy.isEmpty()) {

					endTime = integrateTimesOfEvent(endTimesWithDataSetsDeepCopy, DateType.END, null);

					if (startTime == null || endTime == null)
						break;
					else if (!endTime.before(startTime))
						break;
					else {
						endTimesWithDataSetsDeepCopy.remove(endTime);
						endTime = null;
					}

				} else
					break;
			}

			if (print && endTime != null)
				System.out.println("After, end: " + sdf.format(endTime));

			if (endTime != null)
				DataStore.getInstance().addEndTime(new EndTime(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED_TIME), endTime));

		}
	}

	private void initDataSetsByTrustWorthiness() {
		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.WCE));

		for (Language language : this.languages) {
			if (language != Language.EN)
				dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA));
		}
		// English language most trustworthy
		if (this.languages.contains(Language.EN))
			dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(Language.EN, Source.WIKIPEDIA));

		for (Language language : this.languages) {
			if (language != Language.EN)
				dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
		}
		// English language most trustworthy
		if (this.languages.contains(Language.EN))
			dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));

		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
	}

	private Date integrateTimesOfEvent(Map<Date, Set<DataSet>> timesWithDataSets, DateType dateType,
			List<DataSet> dataSetsByTrustWorthinessCopy) {

		Date dateCase1And2 = getDateCase1And2(timesWithDataSets);

		if (dateCase1And2 != null)
			return dateCase1And2;

		// case 3: after removing Jan 1 / Dec 31: do case 1 and 2 again
		int monthToRemove = 0;
		int dayToRemove = 1;
		if (dateType == DateType.END) {
			monthToRemove = 11;
			dayToRemove = 31;
		}

		Map<Date, Set<DataSet>> timesWithDataSetsWithoutYearStart = new HashMap<Date, Set<DataSet>>();
		for (Date date : timesWithDataSets.keySet()) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);

			if (month == monthToRemove && day == dayToRemove)
				continue;

			timesWithDataSetsWithoutYearStart.put(date, timesWithDataSets.get(date));
		}

		Date dateCase3 = getDateCase1And2(timesWithDataSetsWithoutYearStart);

		if (dateCase3 != null)
			return dateCase3;

		// case 4: majority voting
		Date dateCase4 = getDateCase4(timesWithDataSets);
		if (dateCase4 != null)
			return dateCase4;

		// case 5: rank sources by trustworthiness. Step by step remove the
		// worst source and integrate.
		Map<Date, Set<DataSet>> timesWithoutDataSet = new HashMap<Date, Set<DataSet>>();
		for (Date date : timesWithDataSets.keySet()) {
			for (DataSet dataSetOfDate : timesWithDataSets.get(date)) {
				if (!timesWithoutDataSet.containsKey(date))
					timesWithoutDataSet.put(date, new HashSet<DataSet>());
				timesWithoutDataSet.get(date).add(dataSetOfDate);
			}
		}

		if (dataSetsByTrustWorthinessCopy == null) {
			dataSetsByTrustWorthinessCopy = new ArrayList<DataSet>();
			dataSetsByTrustWorthinessCopy.addAll(dataSetsByTrustWorthiness);
		}

		DataSet dataSetToRemove = null;
		Set<Date> datesOfThatDataset = new HashSet<Date>();
		Set<DataSet> dataSetsToRemoveFromDataSetList = new HashSet<DataSet>();

		for (Iterator<DataSet> it = dataSetsByTrustWorthinessCopy.iterator(); it.hasNext();) {
			DataSet dataSet = it.next();

			if (dataSetToRemove == null)
				dataSetsToRemoveFromDataSetList.add(dataSet);

			for (Date date : timesWithDataSets.keySet()) {
				for (DataSet dataSetOfDate : timesWithDataSets.get(date)) {

					if (dataSetOfDate == dataSet && dataSetToRemove == null) {
						dataSetToRemove = dataSet;
					}

					if (dataSetToRemove != null && dataSetOfDate == dataSetToRemove) {
						datesOfThatDataset.add(date);
					}

				}
			}
		}

		// collect all dates from that data set and remove the latest (start) or
		// earliest (end)
		Date dateToRemove = null;

		if (dateType == DateType.START) {
			for (Date date : datesOfThatDataset) {
				if (dateToRemove == null)
					dateToRemove = date;
				else if (date.after(dateToRemove))
					dateToRemove = date;
			}
		} else if (dateType == DateType.END) {
			for (Date date : datesOfThatDataset) {
				if (dateToRemove == null)
					dateToRemove = date;
				else if (date.before(dateToRemove))
					dateToRemove = date;
			}
		}

		timesWithoutDataSet.get(dateToRemove).remove(dataSetToRemove);

		if (datesOfThatDataset.size() > 1) {
			dataSetsToRemoveFromDataSetList.remove(dataSetToRemove);
		}

		dataSetsByTrustWorthinessCopy.removeAll(dataSetsToRemoveFromDataSetList);

		if (timesWithoutDataSet.get(dateToRemove).isEmpty())
			timesWithoutDataSet.keySet().remove(dateToRemove);

		return integrateTimesOfEvent(timesWithoutDataSet, dateType, dataSetsByTrustWorthinessCopy);
	}

	private Date getDateCase4(Map<Date, Set<DataSet>> timesWithDataSets) {

		boolean strictlyMore = false;
		int maxCount = 0;
		Date dateWithMaxCount = null;

		for (Date date : timesWithDataSets.keySet()) {
			int count = timesWithDataSets.get(date).size();
			if (count > maxCount) {
				strictlyMore = true;
				dateWithMaxCount = date;
				maxCount = timesWithDataSets.get(date).size();
			} else if (count == maxCount) {
				strictlyMore = false;
			}
		}

		if (strictlyMore)
			return dateWithMaxCount;

		return null;
	}

	private Date getDateCase1And2(Map<Date, Set<DataSet>> timesWithDataSets) {

		// case 1: just one time given -> take that
		// case 2: all times are equal -> take that

		if (timesWithDataSets.keySet().size() == 1) {
			for (Date date : timesWithDataSets.keySet())
				return date;
		}
		return null;
	}
}
