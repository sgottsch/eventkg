package de.l3s.eventkg.integration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class TimesIntegrator extends Extractor {

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();

	public TimesIntegrator(List<Language> languages) {
		super("TimesIntegrator", Source.ALL, "Integrate start and end times into a common graph.", languages);
	}

	private enum DateType {
		START,
		END;
	}

	public void run() {
		integrateTimes();
	}

	private void integrateTimes() {

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

		for (Event event : DataStore.getInstance().getEvents()) {

			if (event.getStartTimesWithDataSets() != null && !event.getStartTimesWithDataSets().isEmpty()) {
				Date startTime = integrateTimesOfEvent(event.getStartTimesWithDataSets(), DateType.START, null);
				DataStore.getInstance().addStartTime(new StartTime(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED), startTime));
			}

			List<DataSet> dataSetsByTrustWorthinessCopy2 = new ArrayList<DataSet>();
			dataSetsByTrustWorthinessCopy2.addAll(dataSetsByTrustWorthiness);

			if (event.getEndTimesWithDataSets() != null && !event.getEndTimesWithDataSets().isEmpty()) {
				Date endTime = integrateTimesOfEvent(event.getEndTimesWithDataSets(), DateType.END, null);
				DataStore.getInstance().addEndTime(new EndTime(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED), endTime));
			}

		}
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

		for (Iterator<DataSet> it = dataSetsByTrustWorthinessCopy.iterator(); it.hasNext();) {
			DataSet dataSet = it.next();
			it.remove();
			boolean foundDataSet = false;
			for (Date date : timesWithDataSets.keySet()) {
				for (DataSet dataSetOfDate : timesWithDataSets.get(date)) {

					if (dataSetOfDate == dataSet) {
						foundDataSet = true;
						continue;
					}

				}
				timesWithoutDataSet.get(date).remove(dataSet);
				if (timesWithoutDataSet.get(date).isEmpty())
					timesWithoutDataSet.keySet().remove(date);
			}

			if (foundDataSet)
				break;
		}

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
