package de.l3s.eventkg.integration.integrator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.pipeline.output.RDFWriterName;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class TimesIntegrator extends Extractor {

	private SimpleDateFormat sdf = new SimpleDateFormat("G yyyy-MM-dd", Locale.ENGLISH);

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();

	private List<DateGranularity> dateGranularitiesOrdered;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter triplesWriter;

	private PrintWriter timesWriter;

	public TimesIntegrator(List<Language> languages, TriplesWriter dataStoreWriter,
			WikidataIdMappings wikidataIdMappings) {
		super("TimesIntegrator", Source.ALL, "Fuses start and end times into a common graph.", languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.triplesWriter = dataStoreWriter;
	}

	private enum DateType {
		START,
		END;
	}

	public void run() {

		try {
			this.timesWriter = FileLoader.getWriter(FileName.TIMES_INTEGRATED);
			System.out.println("Integrate times by majority/trust.");
			integrateTimesByTrust();
			this.triplesWriter.getWriter(RDFWriterName.EVENT_BASE_RELATIONS).resetNumberOfLines();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			this.timesWriter.close();
		}

	}

	/**
	 * Remove YAGO as a date source in some specific manually selected cases
	 * where YAGO is simply wrong
	 */
	public void removeWrongTimesFromBlacklist(Entity entity) {

		if (entity.getWikipediaLabel(Language.EN) == null)
			return;

		if (entity.getWikipediaLabel(Language.EN).equals("The Holocaust")
				|| entity.getWikipediaLabel(Language.EN).equals("Christmas")
				|| entity.getWikipediaLabel(Language.EN).equals("Jesus")) {
			for (DateWithGranularity date : entity.getStartTimesWithDataSets().keySet()) {
				if (entity.getStartTimesWithDataSets().containsKey(date)) {
					entity.getStartTimesWithDataSets().get(date)
							.remove(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
				}
			}
			for (DateWithGranularity date : entity.getEndTimesWithDataSets().keySet()) {
				if (entity.getEndTimesWithDataSets().containsKey(date)) {
					entity.getEndTimesWithDataSets().get(date)
							.remove(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
				}
			}
		}
	}

	private void integrateTimesByTrust() {

		System.out.println("integrateTimesByTrust");
		boolean print = false;

		SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		init();

		int i = 0;
		for (Entity entity : this.wikidataIdMappings.getEntitiesByWikidataNumericIds().values()) {
			if (i % 100000 == 0)
				System.out.println("Time integration (step " + i + "). Entity " + i + " ("
						+ logDateFormat.format(new Date()) + ")");
			integrateTimes(entity, print, i);
			i += 1;
		}
	}

	public void init() {
		initDataSetsByTrustWorthiness();
		initDateGranularities();
	}

	public void integrateTimes(Entity entity, boolean print, int i) {

		if ((entity.getStartTimesWithDataSets().isEmpty() || entity.getStartTimesWithDataSets() == null)
				&& (entity.getEndTimesWithDataSets().isEmpty() || entity.getEndTimesWithDataSets() == null))
			return;

		triplesWriter.startInstance();

		for (DateWithGranularity startTime : entity.getStartTimesWithDataSets().keySet()) {
			for (DataSet ds : entity.getStartTimesWithDataSets().get(startTime)) {
				triplesWriter.writeTime(entity, startTime, ds, true, false);
			}
		}
		for (DateWithGranularity endTime : entity.getEndTimesWithDataSets().keySet()) {
			for (DataSet ds : entity.getEndTimesWithDataSets().get(endTime)) {
				triplesWriter.writeTime(entity, endTime, ds, false, false);
			}
		}

		if (print) {
			System.out.println("");

			List<String> beforeStartDates = new ArrayList<String>();
			for (DateWithGranularity d : entity.getStartTimesWithDataSets().keySet()) {
				List<String> dataSets = new ArrayList<String>();
				for (DataSet ds : entity.getStartTimesWithDataSets().get(d))
					dataSets.add(ds.getId());
				beforeStartDates.add(sdf.format(d.getDate()) + "/" + StringUtils.join(dataSets, " "));
			}
			System.out.println("Before, start: " + StringUtils.join(beforeStartDates, " "));
		}

		removeWrongTimesFromBlacklist(entity);

		DateWithGranularity startTime = null;
		if (entity.getStartTimesWithDataSets() != null && !entity.getStartTimesWithDataSets().isEmpty()) {
			startTime = integrateTimesOfEntity(entity.getStartTimesWithDataSets(), DateType.START, null);
			if (startTime != null)
				writeStartTime(entity, startTime);
		}

		if (print && startTime != null)
			System.out.println("After, start: " + sdf.format(startTime.getDate()));

		if (print) {
			List<String> beforeEndDates = new ArrayList<String>();
			for (DateWithGranularity d : entity.getEndTimesWithDataSets().keySet()) {
				List<String> dataSets = new ArrayList<String>();
				for (DataSet ds : entity.getEndTimesWithDataSets().get(d))
					dataSets.add(ds.getId());
				beforeEndDates.add(sdf.format(d.getDate()) + "/" + StringUtils.join(dataSets, " "));
			}
			System.out.println("Before, end: " + StringUtils.join(beforeEndDates, " "));
		}

		// remove the highest ranked end date until we find an end date which is
		// after the start date
		DateWithGranularity endTime = null;
		Map<DateWithGranularity, Set<DataSet>> endTimesWithDataSetsDeepCopy = new HashMap<DateWithGranularity, Set<DataSet>>();
		if (entity.getEndTimesWithDataSets() != null) {
			for (DateWithGranularity date : entity.getEndTimesWithDataSets().keySet()) {
				endTimesWithDataSetsDeepCopy.put(date, new HashSet<DataSet>());
				for (DataSet dataSet : entity.getEndTimesWithDataSets().get(date)) {
					endTimesWithDataSetsDeepCopy.get(date).add(dataSet);
				}
			}
		}

		while (true) {
			if (!endTimesWithDataSetsDeepCopy.isEmpty()) {
				endTime = integrateTimesOfEntity(endTimesWithDataSetsDeepCopy, DateType.END, null);

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
			System.out.println("After, end: " + sdf.format(endTime.getDate()));

		if (endTime != null)
			writeEndTime(entity, endTime);

		triplesWriter.endInstance();
	}

	private void writeStartTime(Entity entity, DateWithGranularity startTime) {
		triplesWriter.writeTime(entity, startTime, DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG),
				true, true);
		timesWriter.println(entity.getWikidataId() + ",B," + sdf.format(startTime.getDate()));
	}

	private void writeEndTime(Entity entity, DateWithGranularity endTime) {
		triplesWriter.writeTime(entity, endTime, DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG),
				false, true);
		timesWriter.println(entity.getWikidataId() + ",E," + sdf.format(endTime.getDate()));
	}

	private DateWithGranularity integrateTimesOfEntity(Map<DateWithGranularity, Set<DataSet>> timesWithDataSets,
			DateType dateType, List<DataSet> dataSetsByTrustWorthinessCopy) {

		DateWithGranularity dateCase1And2 = getDateCase1And2(timesWithDataSets);

		if (dateCase1And2 != null)
			return dateCase1And2;

		// case 3: try case 1 and 2 when ignoring inexact dates. First, discard
		// all dates that are not days, then all that are not days or months,
		// ...

		for (DateGranularity granularity : dateGranularitiesOrdered) {

			Map<DateWithGranularity, Set<DataSet>> timesWithDataSetsWithoutYearStart = new HashMap<DateWithGranularity, Set<DataSet>>();
			for (DateWithGranularity date : timesWithDataSets.keySet()) {

				if (date.getGranularity().isLessExactThan(granularity))
					continue;

				timesWithDataSetsWithoutYearStart.put(date, timesWithDataSets.get(date));
			}

			DateWithGranularity dateCase3 = getDateCase1And2(timesWithDataSetsWithoutYearStart);
			if (dateCase3 != null)
				return dateCase3;
		}

		// case 4: majority voting
		DateWithGranularity dateCase4 = getDateCase4(timesWithDataSets);
		if (dateCase4 != null)
			return dateCase4;

		// case 5: rank sources by trustworthiness. Step by step remove the
		// worst source and integrate.
		Map<DateWithGranularity, Set<DataSet>> timesWithoutDataSet = new HashMap<DateWithGranularity, Set<DataSet>>();
		for (DateWithGranularity date : timesWithDataSets.keySet()) {
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
		Set<DateWithGranularity> datesOfThatDataset = new HashSet<DateWithGranularity>();
		Set<DataSet> dataSetsToRemoveFromDataSetList = new HashSet<DataSet>();

		for (Iterator<DataSet> it = dataSetsByTrustWorthinessCopy.iterator(); it.hasNext();) {
			DataSet dataSet = it.next();

			if (dataSetToRemove == null)
				dataSetsToRemoveFromDataSetList.add(dataSet);

			for (DateWithGranularity date : timesWithDataSets.keySet()) {
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
		DateWithGranularity dateToRemove = null;

		if (dateType == DateType.START) {
			for (DateWithGranularity date : datesOfThatDataset) {
				if (dateToRemove == null)
					dateToRemove = date;
				else if (date.after(dateToRemove))
					dateToRemove = date;
			}
		} else if (dateType == DateType.END) {
			for (DateWithGranularity date : datesOfThatDataset) {
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

		return integrateTimesOfEntity(timesWithoutDataSet, dateType, dataSetsByTrustWorthinessCopy);
	}

	private DateWithGranularity getDateCase4(Map<DateWithGranularity, Set<DataSet>> timesWithDataSets) {

		boolean strictlyMore = false;
		int maxCount = 0;
		DateWithGranularity dateWithMaxCount = null;

		for (DateWithGranularity date : timesWithDataSets.keySet()) {
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

	private DateWithGranularity getDateCase1And2(Map<DateWithGranularity, Set<DataSet>> timesWithDataSets) {

		// case 1: just one time given -> take that
		// case 2: all times are equal -> take that

		if (timesWithDataSets.keySet().size() == 1) {
			for (DateWithGranularity date : timesWithDataSets.keySet())
				return date;
		}
		return null;
	}

	private void initDataSetsByTrustWorthiness() {
		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(Language.EN, Source.WCE));

		// TODO: Not working? Check example
		// "Withdrawal_of_U.S._troops_from_Iraq"@en:
		// http://eventkginterface.l3s.uni-hannover.de/sparql?default-graph-uri=&query=PREFIX+eventKG-s%3A+%3Chttp%3A%2F%2FeventKG.l3s.uni-hannover.de%2Fschema%2F%3E%0D%0APREFIX+rdf%3A+%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0D%0APREFIX+so%3A+%3Chttp%3A%2F%2Fschema.org%2F%3E%0D%0A%0D%0ASELECT+%3Fstart+%3Fg%0D%0AWHERE%0D%0A%7B%0D%0A+%3Fevent+rdf%3Atype+eventKG-s%3AEvent+.%0D%0A+%3Fevent+rdf%3Alabel+%22Withdrawal_of_U.S._troops_from_Iraq%22%40en+.%0D%0AGRAPH+%3Fg+%7B%3Fevent+so%3AstartTime+%3Fstart.%7D%0D%0A%7D%0D%0A&format=text%2Fhtml&timeout=0&debug=on

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

	private void initDateGranularities() {
		this.dateGranularitiesOrdered = new ArrayList<DateGranularity>();
		this.dateGranularitiesOrdered.add(DateGranularity.DAY);
		this.dateGranularitiesOrdered.add(DateGranularity.MONTH);
		this.dateGranularitiesOrdered.add(DateGranularity.YEAR);
		this.dateGranularitiesOrdered.add(DateGranularity.DECADE);
	}

}
