package de.l3s.eventkg.evaluation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.util.MapUtil;

public class TimeFusionEvaluationResults {

	private Map<String, DataSet> dataSets = new HashMap<String, DataSet>();

	private Map<String, Event> events = new HashMap<String, Event>();

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public static void main(String[] args) {
		TimeFusionEvaluationResults tfer = new TimeFusionEvaluationResults();

		String dataFileName = "/home/simon/Documents/EventKB/time_evaluation_data/time_fusion_all.tsv";
		String evaluationDataFolderName = "/home/simon/Documents/EventKB/time_evaluation_data/results/";

		tfer.loadData(dataFileName);
		tfer.loadEvaluationData(evaluationDataFolderName);
		tfer.evaluate();
	}

	private void loadData(String dataFileName) {

		LineIterator it = null;
		try {
			it = FileUtils.lineIterator(new File(dataFileName), "UTF-8");
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split("\t");
				Event event = new Event();
				event.setId(parts[0]);
				for (int i = 1; i < parts.length; i++) {
					String[] parts2 = parts[i].split(";");
					String sources = parts2[0];

					DateWithGranularity startDate = null;
					DateWithGranularity endDate = null;
					try {
						if (!parts2[1].equals("-"))
							startDate = new DateWithGranularity(TimeFusionEvaluation.dateFormat.parse(parts2[1]),
									DateGranularity.DAY);
						if (!parts2[2].equals("-"))
							endDate = new DateWithGranularity(TimeFusionEvaluation.dateFormat.parse(parts2[2]),
									DateGranularity.DAY);
					} catch (ParseException e) {
						e.printStackTrace();
					}

					for (String source : sources.split(", ")) {
						event.addStartTime(startDate, getDataSet(source));
						event.addEndTime(endDate, getDataSet(source));
					}

					events.put(event.getId(), event);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void loadEvaluationData(String evaluationDataFolderName) {

		File dir = new File(evaluationDataFolderName);
		File[] directoryListing = dir.listFiles();

		Map<String, Integer> sources = new HashMap<String, Integer>();
		int sum = 0;

		for (File file : directoryListing) {
			LineIterator it = null;
			try {
				it = FileUtils.lineIterator(file, "UTF-8");
				boolean firstLine = true;
				while (it.hasNext()) {
					String line = it.nextLine();

					if (firstLine) {
						firstLine = false;
						continue;
					}

					String[] parts = line.split("\t");
					Event event = events.get(parts[0]);

					String source1 = getSource(parts[2]);
					String source2 = getSource(parts[4]);

					if (!sources.containsKey(source1))
						sources.put(source1, 0);
					if (!sources.containsKey(source2))
						sources.put(source2, 0);

					sources.put(source1, sources.get(source1) + 1);
					sources.put(source2, sources.get(source2) + 1);
					sum += 2;

					DateWithGranularity startDate = null;
					DateWithGranularity endDate = null;

					try {
						startDate = new DateWithGranularity(dateFormat.parse(parts[1]), DateGranularity.DAY);
					} catch (ParseException e) {
						System.err.println("Unparsebale date: " + parts[1]);
						continue;
					}
					try {
						endDate = new DateWithGranularity(dateFormat.parse(parts[3]), DateGranularity.DAY);
					} catch (ParseException e) {
						System.err.println("Unparsebale date: " + parts[3]);
						continue;
					}
					event.addStartTime(startDate, getDataSet("user"));
					event.addEndTime(endDate, getDataSet("user"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println(sum);
		System.out.println("--- Sources ---");
		for (String source : MapUtil.sortByValueDescending(sources).keySet()) {
			System.out.println(source + "\t" + sources.get(source) + "\t" + (double) sources.get(source) / sum);
		}

		System.out.println("");

	}

	public static String getSource(String source) {

		URL url;
		try {
			url = new URL(source);
			return url.getHost();
		} catch (MalformedURLException e) {
		}

		return "-";
	}

	private void evaluate() {

		// filter s.t. only events with user annotation remain
		for (Iterator<String> it = events.keySet().iterator(); it.hasNext();) {
			String label = it.next();
			if (events.get(label).getDataSetsWithStartTimes().get(getDataSet("user")) == null)
				it.remove();
		}
		System.out.println("#Events with annotations: " + events.size());

		Map<DataSet, Integer> correctDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> correctStartDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> correctEndDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> wrongDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> wrongStartDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> wrongEndDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> missingDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> missingStartDates = new HashMap<DataSet, Integer>();
		Map<DataSet, Integer> missingEndDates = new HashMap<DataSet, Integer>();

		for (DataSet dataSet : this.dataSets.values()) {
			correctStartDates.put(dataSet, 0);
			correctDates.put(dataSet, 0);
			correctEndDates.put(dataSet, 0);
			wrongDates.put(dataSet, 0);
			wrongStartDates.put(dataSet, 0);
			wrongEndDates.put(dataSet, 0);
			missingDates.put(dataSet, 0);
			missingStartDates.put(dataSet, 0);
			missingEndDates.put(dataSet, 0);
		}

		Map<Event, Map<DataSet, Boolean>> correctInDataSet = new HashMap<Event, Map<DataSet, Boolean>>();

		for (Event event : events.values()) {
			correctInDataSet.put(event, new HashMap<DataSet, Boolean>());
			for (DataSet dataSet : this.dataSets.values()) {
				DateWithGranularity userStartDate = event.getDataSetsWithStartTimes().get(getDataSet("user"));
				DateWithGranularity dataStartDate = event.getDataSetsWithStartTimes().get(dataSet);
				if (userStartDate != null && dataStartDate != null && userStartDate.equals(dataStartDate)) {
					correctStartDates.put(dataSet, correctStartDates.get(dataSet) + 1);
					correctDates.put(dataSet, correctDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, true);
				} else if (dataStartDate == null) {
					missingStartDates.put(dataSet, missingStartDates.get(dataSet) + 1);
					missingDates.put(dataSet, missingDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, false);
				} else {
					wrongStartDates.put(dataSet, wrongStartDates.get(dataSet) + 1);
					wrongDates.put(dataSet, wrongDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, false);
				}

				DateWithGranularity userEndDate = event.getDataSetsWithEndTimes().get(getDataSet("user"));
				DateWithGranularity dataEndDate = event.getDataSetsWithEndTimes().get(dataSet);
				if (userEndDate != null && dataEndDate != null && userEndDate.equals(dataEndDate)) {
					correctEndDates.put(dataSet, correctEndDates.get(dataSet) + 1);
					correctDates.put(dataSet, correctDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, true);
				} else if (dataEndDate == null) {
					missingEndDates.put(dataSet, missingEndDates.get(dataSet) + 1);
					missingDates.put(dataSet, missingDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, false);
				} else {
					wrongEndDates.put(dataSet, wrongEndDates.get(dataSet) + 1);
					wrongDates.put(dataSet, wrongDates.get(dataSet) + 1);
					correctInDataSet.get(event).put(dataSet, false);
				}

			}
		}

		System.out.println("correct\twrong\tmissing");
		System.out.println("--- Start Dates ---");
		for (DataSet dataSet : this.dataSets.values()) {
			System.out.println(dataSet.getId() + "\t" + correctStartDates.get(dataSet) + "\t"
					+ wrongStartDates.get(dataSet) + "\t" + missingStartDates.get(dataSet));
		}
		System.out.println("");

		System.out.println("--- End Dates ---");
		for (DataSet dataSet : this.dataSets.values()) {
			System.out.println(dataSet.getId() + "\t" + correctEndDates.get(dataSet) + "\t" + wrongEndDates.get(dataSet)
					+ "\t" + missingEndDates.get(dataSet));
		}
		System.out.println("");

		System.out.println("--- Dates ---");
		for (DataSet dataSet : this.dataSets.values()) {
			System.out.println(dataSet.getId() + "\t" + correctDates.get(dataSet) + "\t" + wrongDates.get(dataSet)
					+ "\t" + missingDates.get(dataSet));
		}

		System.out.println("--- Alll ---");
		for (DataSet dataSet : this.dataSets.values()) {
			System.out.println("\\multicolumn{1}{|l||}{" + dataSet.getId() + "} & " + +correctStartDates.get(dataSet)
					+ " & " + wrongStartDates.get(dataSet) + " & " + missingStartDates.get(dataSet) + " & "
					+ correctEndDates.get(dataSet) + " & " + wrongEndDates.get(dataSet) + " & "
					+ missingEndDates.get(dataSet) + " & " + correctDates.get(dataSet) + " & " + wrongDates.get(dataSet)
					+ " & " + missingDates.get(dataSet) + " \\\\ \\hline");
		}

		int d1Cd2C = 0;
		int d1Cd2W = 0;
		int d1Wd2C = 0;
		int d1Wd2W = 0;
		DataSet dataSet1 = this.dataSets.get("wikidata");
		DataSet dataSet2 = this.dataSets.get("event_kg");

		for (Event event : events.values()) {
			if (correctInDataSet.get(event).get(dataSet1) && correctInDataSet.get(event).get(dataSet2))
				d1Cd2C += 1;
			else if (correctInDataSet.get(event).get(dataSet1) && !correctInDataSet.get(event).get(dataSet2))
				d1Cd2W += 1;
			else if (!correctInDataSet.get(event).get(dataSet1) && correctInDataSet.get(event).get(dataSet2))
				d1Wd2C += 1;
			else if (!correctInDataSet.get(event).get(dataSet1) && !correctInDataSet.get(event).get(dataSet2))
				d1Wd2W += 1;
		}

		System.out.println("McNemar's test:");
		System.out.println("\t" + dataSet2.getId() + ":correct\t" + dataSet2.getId() + ":wrong");
		System.out.println(dataSet1.getId() + ":correct\t" + d1Cd2C + "\t" + d1Cd2W);
		System.out.println(dataSet1.getId() + ":wrong\t" + d1Wd2C + "\t" + d1Wd2W);

	}

	private DataSet getDataSet(String sourceId) {
		sourceId = sourceId.replace("eventKG-g:", "");
		DataSet dataSet = this.dataSets.get(sourceId);

		if (dataSet != null)
			return dataSet;

		Source source = null;

		// switch (sourceId) {
		// case "event_kg":
		// source=Source.EVENT_KG;
		// break;
		// case "yago":
		// source=Source.YAGO;
		// break;
		// case "event_kg":
		// source=Source.EVENT_KG;
		// break;
		// case "event_kg":
		// source=Source.EVENT_KG;
		// break;
		//
		// default:
		// break;
		// }

		dataSet = new DataSet(source, sourceId, null);
		dataSets.put(sourceId, dataSet);

		return dataSet;
	}

}
