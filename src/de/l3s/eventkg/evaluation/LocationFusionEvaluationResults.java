package de.l3s.eventkg.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.util.MapUtil;

public class LocationFusionEvaluationResults {

//	private Map<String, DataSet> dataSets = new HashMap<String, DataSet>();
//
//	private Map<String, Event> events = new HashMap<String, Event>();
//	private Map<String, Entity> entities = new HashMap<String, Entity>();
//
//	public static void main(String[] args) {
//		LocationFusionEvaluationResults lfer = new LocationFusionEvaluationResults();
//
//		String dataFileName = "/home/simon/Documents/EventKB/location_evaluation_data/location_fusion_all.tsv";
//		String evaluationDataFolderName = "/home/simon/Documents/EventKB/location_evaluation_data/results/";
//
//		lfer.loadData(dataFileName);
//		lfer.loadEvaluationData(evaluationDataFolderName);
//		lfer.evaluate();
//	}
//
//	private void loadData(String dataFileName) {
//		LineIterator it = null;
//		try {
//			it = FileUtils.lineIterator(new File(dataFileName), "UTF-8");
//			while (it.hasNext()) {
//				String line = it.nextLine();
//				String[] parts = line.split("\t");
//				Event event = getEvent(parts[0]);
//
//				String locationName = parts[2];
//				Entity location = getEntity(locationName);
//
//				String sources = parts[1];
//				for (String source : sources.split(";")) {
//					event.addLocation(location, getDataSet(source));
//				}
//				event.addLocation(location, getDataSet("event_kg*"));
//
//				events.put(event.getId(), event);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				it.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	private Entity getEntity(String locationName) {
//		if (this.entities.containsKey(locationName))
//			return this.entities.get(locationName);
//
//		Entity entity = new Entity();
//		entity.setId(locationName);
//		this.entities.put(locationName, entity);
//
//		return entity;
//	}
//
//	private Event getEvent(String name) {
//		if (this.events.containsKey(name))
//			return this.events.get(name);
//
//		Event event = new Event();
//		event.setId(name);
//		this.events.put(name, event);
//
//		return event;
//	}
//
//	private void loadEvaluationData(String evaluationDataFolderName) {
//		System.out.println("loadEvaluationData");
//
//		File dir = new File(evaluationDataFolderName);
//		File[] directoryListing = dir.listFiles();
//
//		Map<String, Integer> sources = new HashMap<String, Integer>();
//		int sum = 0;
//
//		for (File file : directoryListing) {
//			LineIterator it = null;
//			try {
//				it = FileUtils.lineIterator(file, "UTF-8");
//				boolean firstLine = true;
//				while (it.hasNext()) {
//					String line = it.nextLine();
//
//					if (firstLine) {
//						firstLine = false;
//						continue;
//					}
//
//					String[] parts = line.split("\t");
//
//					if (parts.length == 0 || parts[0].isEmpty())
//						continue;
//
//					String source = "-";
//
//					if (parts.length > 3)
//						source = TimeFusionEvaluationResults.getSource(parts[3]);
//
//					sum += 1;
//
//					if (!sources.containsKey(source))
//						sources.put(source, 0);
//					sources.put(source, sources.get(source) + 1);
//
//					if (parts[2].equals("y")) {
//						events.get(parts[0]).addLocation(getEntity(parts[1]), getDataSet("user"));
//					}
//
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				try {
//					it.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		System.out.println(sum);
//		System.out.println("--- Sources ---");
//		for (String source : MapUtil.sortByValueDescending(sources).keySet()) {
//			System.out.println(source + "\t" + sources.get(source) + "\t" + (double) sources.get(source) / sum);
//		}
//
//		System.out.println("");
//	}
//
//	private void evaluate() {
//
//		// filter s.t. only events with user annotation remain
//		for (Iterator<String> it = events.keySet().iterator(); it.hasNext();) {
//			String label = it.next();
//			if (events.get(label).getDataSetsWithLocations().get(getDataSet("user")) == null)
//				it.remove();
//		}
//		System.out.println("#Events with annotations: " + events.size());
//
//		Map<DataSet, Integer> correctLocations = new HashMap<DataSet, Integer>();
//		Map<DataSet, Integer> wrongLocations = new HashMap<DataSet, Integer>();
//		for (DataSet dataSet : this.dataSets.values()) {
//			correctLocations.put(dataSet, 0);
//			wrongLocations.put(dataSet, 0);
//		}
//
//		Map<Event, Map<DataSet, Boolean>> correctInDataSet = new HashMap<Event, Map<DataSet, Boolean>>();
//
//		for (Event event : events.values()) {
//			correctInDataSet.put(event, new HashMap<DataSet, Boolean>());
//
//			for (DataSet dataSet : this.dataSets.values()) {
//				correctInDataSet.get(event).put(dataSet, false);
//			}
//
//			for (DataSet dataSet : event.getDataSetsWithLocations().keySet()) {
//				boolean foundACorrectLocation = false;
//				boolean foundAFalseLocation = false;
//				for (Entity location : event.getDataSetsWithLocations().get(dataSet)) {
//					if (!event.getDataSetsWithLocations().get(getDataSet("user")).contains(location)) {
//						wrongLocations.put(dataSet, wrongLocations.get(dataSet) + 1);
//						foundAFalseLocation = true;
//					} else {
//						correctLocations.put(dataSet, correctLocations.get(dataSet) + 1);
//						foundACorrectLocation = true;
//					}
//				}
//				correctInDataSet.get(event).put(dataSet, foundACorrectLocation && !foundAFalseLocation);
//			}
//		}
//
//		System.out.println("dataset\tcorrect\twrong");
//		for (DataSet dataSet : MapUtil.sortByValueDescending(correctLocations).keySet()) {
//			System.out.println(
//					dataSet.getId() + "\t" + correctLocations.get(dataSet) + "\t" + wrongLocations.get(dataSet));
//		}
//
//		for (DataSet dataSet : MapUtil.sortByValueDescending(correctLocations).keySet()) {
//			System.out.println(dataSet.getId() + " & " + correctLocations.get(dataSet) + " & "
//					+ wrongLocations.get(dataSet) + " \\\\ \\hline");
//		}
//
//		int d1Cd2C = 0;
//		int d1Cd2W = 0;
//		int d1Wd2C = 0;
//		int d1Wd2W = 0;
//		DataSet dataSet1 = this.dataSets.get("yago");
//		DataSet dataSet2 = this.dataSets.get("event_kg*");
//
//		for (Event event : events.values()) {
//			System.out.println(correctInDataSet.get(event));
//			System.out.println(dataSet1.getId());
//			System.out.println(correctInDataSet.get(event).get(dataSet1));
//			if (correctInDataSet.get(event).get(dataSet1) && correctInDataSet.get(event).get(dataSet2))
//				d1Cd2C += 1;
//			else if (correctInDataSet.get(event).get(dataSet1) && !correctInDataSet.get(event).get(dataSet2))
//				d1Cd2W += 1;
//			else if (!correctInDataSet.get(event).get(dataSet1) && correctInDataSet.get(event).get(dataSet2))
//				d1Wd2C += 1;
//			else if (!correctInDataSet.get(event).get(dataSet1) && !correctInDataSet.get(event).get(dataSet2))
//				d1Wd2W += 1;
//		}
//
//		System.out.println("McNemar's test:");
//		System.out.println("\t" + dataSet2.getId() + ":correct\t" + dataSet2.getId() + ":wrong");
//		System.out.println(dataSet1.getId() + ":correct\t" + d1Cd2C + "\t" + d1Cd2W);
//		System.out.println(dataSet1.getId() + ":wrong\t" + d1Wd2C + "\t" + d1Wd2W);
//
//	}
//
//	private DataSet getDataSet(String sourceId) {
//		sourceId = sourceId.replace("eventKG-g:", "");
//		DataSet dataSet = this.dataSets.get(sourceId);
//
//		if (dataSet != null)
//			return dataSet;
//
//		Source source = null;
//
//		// switch (sourceId) {
//		// case "event_kg":
//		// source=Source.EVENT_KG;
//		// break;
//		// case "yago":
//		// source=Source.YAGO;
//		// break;
//		// case "event_kg":
//		// source=Source.EVENT_KG;
//		// break;
//		// case "event_kg":
//		// source=Source.EVENT_KG;
//		// break;
//		//
//		// default:
//		// break;
//		// }
//
//		dataSet = new DataSet(source, sourceId, null);
//		dataSets.put(sourceId, dataSet);
//
//		return dataSet;
//	}

}
