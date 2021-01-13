package de.l3s.eventkg.evaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LocationEvaluation {

//	Map<String, Event> events = new HashMap<String, Event>();
//	Map<String, Entity> entities = new HashMap<String, Entity>();
//	Map<String, DataSet> dataSets = new HashMap<String, DataSet>();
//	Map<Event, Map<Language, Set<String>>> eventLabels = new HashMap<Event, Map<Language, Set<String>>>();
//	Map<Entity, Map<Language, Set<String>>> entityLabels = new HashMap<Entity, Map<Language, Set<String>>>();
//
//	List<Language> languagesByPrio = new ArrayList<Language>();
//
//	private boolean compare;
//
//	public LocationEvaluation(boolean compare) {
//		this.compare = compare;
//
//		languagesByPrio.add(Language.EN);
//		languagesByPrio.add(Language.DE);
//		languagesByPrio.add(Language.FR);
//		languagesByPrio.add(Language.PT);
//		languagesByPrio.add(Language.RU);
//	}
//
//	public static void main(String[] args) {
//
//		Config.init(args[0]);
//
//		boolean compare = false;
//		if (args.length > 1 && args[1].equals("compare"))
//			compare = true;
//
//		LocationEvaluation tfe = new LocationEvaluation(compare);
//		tfe.loadEvents();
//		tfe.filterEvents();
//		tfe.writeSample();
//	}
//
//	private void writeSample() {
//
//		List<Event> eventsSample = new ArrayList<Event>();
//		eventsSample.addAll(this.events.values());
//
//		Collections.shuffle(eventsSample);
//		eventsSample = eventsSample.subList(0, 120);
//
//		System.out.println("Sampled events: " + eventsSample.size());
//
//		String folderName = "location_evaluation_data/";
//
//		List<PrintWriter> writerSheets = new ArrayList<PrintWriter>();
//		PrintWriter writerAllData = null;
//		try {
//			writerAllData = new PrintWriter(folderName + "location_fusion_all.tsv");
//
//			PrintWriter writerSheet = null;
//
//			int i = 0;
//			eventLoop: for (Event event : eventsSample) {
//				String label = createLabel(event);
//				if (label == null)
//					continue;
//
//				if (i % 20 == 0) {
//					writerSheet = new PrintWriter(folderName + "sheet_" + ((int) Math.floor(i / 20)) + ".tsv");
//					writerSheet.write("Event\tLocation\tYes (y) or no (n)?\tSource (Link)\tNote\n");
//					writerSheets.add(writerSheet);
//				}
//
//				i += 1;
//
//				Map<String, Set<DataSet>> locationLabels = new HashMap<String, Set<DataSet>>();
//
//				Map<DataSet, String> bySource = new HashMap<DataSet, String>();
//
//				for (DataSet dataSet : this.dataSets.values())
//					bySource.put(dataSet, "@loc");
//
//				for (Entity location : event.getLocationsWithDataSets().keySet()) {
//					for (DataSet dataSet : event.getLocationsWithDataSets().get(location)) {
//						String locationLabel = createLabel(location);
//						if (locationLabel == null)
//							continue eventLoop;
//						bySource.put(dataSet, bySource.get(dataSet).replace("@loc", locationLabel));
//					}
//				}
//
//				for (DataSet dataSet : bySource.keySet()) {
//
//					if (bySource.get(dataSet).contains("@loc"))
//						continue;
//
//					if (!locationLabels.containsKey(bySource.get(dataSet))) {
//						locationLabels.put(bySource.get(dataSet), new HashSet<DataSet>());
//					}
//					locationLabels.get(bySource.get(dataSet)).add(dataSet);
//				}
//
//				for (String locationLabel : locationLabels.keySet()) {
//					// if (!dateFormatted.endsWith("]"))
//					// dateFormatted = dateFormatted + "]";
//					// dateFormatted = dateFormatted.replace(",]", "]");
//
//					List<String> sources = new ArrayList<String>();
//					for (DataSet dataSet : locationLabels.get(locationLabel))
//						sources.add(dataSet.getId());
//					Collections.sort(sources);
//
//					writerSheet.write(label + "\t" + locationLabel + "\n");
//					writerAllData.write(label + "\t" + StringUtils.join(sources, ";") + "\t" + locationLabel + "\n");
//				}
//
//				if (i == 100)
//					break;
//				writerSheet.write("\n");
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} finally {
//			writerAllData.close();
//			for (PrintWriter writer : writerSheets)
//				writer.close();
//		}
//
//	}
//
//	private String createLabel(Event event) {
//
//		Map<Language, Set<String>> labels = this.eventLabels.get(event);
//		for (Language language : this.languagesByPrio) {
//			if (labels.containsKey(language)) {
//				List<String> languageLabels = new ArrayList<String>();
//				languageLabels.addAll(labels.get(language));
//				return StringUtils.join(languageLabels, " / ");
//			}
//		}
//
//		return null;
//	}
//
//	private String createLabel(Entity entity) {
//
//		Map<Language, Set<String>> labels = this.entityLabels.get(entity);
//		if (labels == null)
//			return null;
//
//		for (Language language : this.languagesByPrio) {
//			if (labels.containsKey(language)) {
//				List<String> languageLabels = new ArrayList<String>();
//				languageLabels.addAll(labels.get(language));
//				return StringUtils.join(languageLabels, " / ");
//			}
//		}
//
//		return null;
//	}
//
//	private void filterEvents() {
//
//		for (Iterator<String> it = this.events.keySet().iterator(); it.hasNext();) {
//			String eventId = it.next();
//			Event event = this.events.get(eventId);
//
//			if (compare) {
//				// Set<DataSet> dataSets = new HashSet<DataSet>();
//				//
//				// for (Date date : event.getStartTimesWithDataSets().keySet())
//				// dataSets.addAll(event.getStartTimesWithDataSets().get(date));
//				// for (Date date : event.getEndTimesWithDataSets().keySet())
//				// dataSets.addAll(event.getEndTimesWithDataSets().get(date));
//
//				if (event.getLocations().size() <= 2)
//					it.remove();
//			} else {
//				if (event.getLocations().isEmpty())
//					it.remove();
//			}
//		}
//
//		System.out.println("Filtered events: " + this.events.size());
//
//	}
//
//	private void loadEvents() {
//
//		System.out.println("loadEvents");
//
//		int i = 0;
//		LineIterator it = null;
//		try {
//			it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
//			while (it.hasNext()) {
//				String line = it.nextLine();
//
//				if (i % 100000 == 0)
//					System.out.println("Line " + i);
//				i += 1;
//
//				if (!line.startsWith("<event"))
//					continue;
//
//				if (line.contains("sem:hasPlace")) {
//					String[] parts = line.split(" ");
//					Event event = getEvent(parts[0]);
//					String source = parts[3];
//					Entity locationEntity = getEntity(parts[2]);
//
//					event.addLocation(locationEntity, getDataSet(source));
//				}
//
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
//
//		loadLabels();
//		loadLocationLabels();
//
//		System.out.println("Loaded events: " + this.events.size());
//
//	}
//
//	private void loadLabels() {
//
//		System.out.println("Load labels");
//
//		int i = 0;
//		LineIterator it = null;
//		try {
//			it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
//			while (it.hasNext()) {
//				String line = it.nextLine();
//
//				if (i % 100000 == 0)
//					System.out.println("Line " + i);
//				i += 1;
//
//				if (!line.contains("rdfs:label"))
//					continue;
//
//				String eventId = line.substring(0, line.indexOf(" "));
//				if (!this.events.containsKey(eventId))
//					continue;
//
//				line = line.substring(line.indexOf(" ") + 1);
//				line = line.substring(line.indexOf(" ") + 1);
//
//				String label = line.substring(0, line.indexOf(" eventKG-g:"));
//
//				Event event = this.events.get(eventId);
//				Language language = Language.valueOf(label.substring(label.length() - 2).toUpperCase());
//
//				label = label.substring(1, label.indexOf("@") - 1);
//
//				if (!eventLabels.containsKey(event))
//					eventLabels.put(event, new HashMap<Language, Set<String>>());
//				if (!eventLabels.get(event).containsKey(language))
//					eventLabels.get(event).put(language, new HashSet<String>());
//
//				eventLabels.get(event).get(language).add(label);
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
//
//	}
//
//	private void loadLocationLabels() {
//
//		System.out.println("Load labels");
//
//		int i = 0;
//		LineIterator it = null;
//		try {
//			it = FileLoader.getLineIterator(FileName.ALL_TTL_ENTITIES_WITH_TEXTS);
//			while (it.hasNext()) {
//				String line = it.nextLine();
//
//				if (i % 100000 == 0)
//					System.out.println("Line " + i);
//				i += 1;
//
//				if (!line.contains("rdfs:label"))
//					continue;
//
//				String entityId = line.substring(0, line.indexOf(" "));
//				if (!this.entities.containsKey(entityId))
//					continue;
//
//				line = line.substring(line.indexOf(" ") + 1);
//				line = line.substring(line.indexOf(" ") + 1);
//
//				String label = line.substring(0, line.indexOf(" eventKG-g:"));
//
//				Entity event = this.entities.get(entityId);
//				Language language = Language.valueOf(label.substring(label.length() - 2).toUpperCase());
//
//				label = label.substring(1, label.indexOf("@") - 1);
//
//				if (!entityLabels.containsKey(event))
//					entityLabels.put(event, new HashMap<Language, Set<String>>());
//				if (!entityLabels.get(event).containsKey(language))
//					entityLabels.get(event).put(language, new HashSet<String>());
//
//				entityLabels.get(event).get(language).add(label);
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
//		System.out.println("New dataSet: " + sourceId);
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
//
//	private Event getEvent(String eventId) {
//
//		Event event = this.events.get(eventId);
//
//		if (event != null)
//			return event;
//
//		event = new Event();
//		event.setId(eventId);
//
//		this.events.put(eventId, event);
//
//		return event;
//	}
//
//	private Entity getEntity(String entityId) {
//
//		Entity entity = this.entities.get(entityId);
//
//		if (entity != null)
//			return entity;
//
//		entity = new Entity();
//		entity.setId(entityId);
//
//		this.entities.put(entityId, entity);
//
//		return entity;
//	}

}
