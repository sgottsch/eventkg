package de.l3s.eventkg.evaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;
import edu.stanford.nlp.util.StringUtils;

public class TimeFusionEvaluation {

	Map<String, Event> events = new HashMap<String, Event>();
	Map<String, DataSet> dataSets = new HashMap<String, DataSet>();
	Map<Event, Map<Language, Set<String>>> eventLabels = new HashMap<Event, Map<Language, Set<String>>>();

	List<Language> languagesByPrio = new ArrayList<Language>();

	private boolean compare;
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");

	public TimeFusionEvaluation(boolean compare) {
		this.compare = compare;

		languagesByPrio.add(Language.EN);
		languagesByPrio.add(Language.DE);
		languagesByPrio.add(Language.FR);
		languagesByPrio.add(Language.PT);
		languagesByPrio.add(Language.RU);
	}

	public static void main(String[] args) {

		Config.init(args[0]);

		boolean compare = false;
		if (args.length > 1 && args[1].equals("compare"))
			compare = true;

		TimeFusionEvaluation tfe = new TimeFusionEvaluation(compare);
		tfe.loadEvents();
		tfe.filterEvents();
		tfe.writeSample();
	}

	private void writeSample() {

		List<Event> eventsSample = new ArrayList<Event>();
		eventsSample.addAll(this.events.values());

		Collections.shuffle(eventsSample);
		eventsSample = eventsSample.subList(0, 120);

		System.out.println("Sampled events: " + eventsSample.size());

		String folderName = "time_evaluation_data/";

		List<PrintWriter> writerSheets = new ArrayList<PrintWriter>();
		PrintWriter writerAllData = null;
		try {
			writerAllData = new PrintWriter(folderName + "time_fusion_all.tsv");

			PrintWriter writerSheet = null;

			int i = 0;
			for (Event event : eventsSample) {
				String label = createLabel(event);
				if (label == null)
					continue;

				if (i % 20 == 0) {
					writerSheet = new PrintWriter(folderName + "sheet_" + ((int) Math.floor(i / 20)) + ".tsv");
					writerSheet.write(
							"Event\tStart time (yyyy-MM-dd, e.g. 2011-12-24)\tSource (Link)\tEnd time (yyyy-MM-dd, e.g. 2011-12-24)\tSource (Link)\n");
					writerSheets.add(writerSheet);
				}

				System.out.println(label);
				writerAllData.write(label);
				writerSheet.write(label + "\n");

				i += 1;

				Map<String, Set<DataSet>> startEndTimeCombinations = new HashMap<String, Set<DataSet>>();

				Map<DataSet, String> bySource = new HashMap<DataSet, String>();

				for (DataSet dataSet : this.dataSets.values())
					bySource.put(dataSet, "@st;@et");

				for (DateWithGranularity date : event.getStartTimesWithDataSets().keySet()) {
					for (DataSet dataSet : event.getStartTimesWithDataSets().get(date)) {
						bySource.put(dataSet, bySource.get(dataSet).replace("@st", dateFormat.format(date)));
					}
				}

				for (DateWithGranularity date : event.getEndTimesWithDataSets().keySet()) {
					for (DataSet dataSet : event.getEndTimesWithDataSets().get(date)) {
						bySource.put(dataSet, bySource.get(dataSet).replace("@et", dateFormat.format(date)));
					}
				}

				for (DataSet dataSet : bySource.keySet()) {

					if (bySource.get(dataSet).contains("@st") && bySource.get(dataSet).contains("@et"))
						continue;

					if (!startEndTimeCombinations.containsKey(bySource.get(dataSet))) {
						startEndTimeCombinations.put(bySource.get(dataSet), new HashSet<DataSet>());
					}
					startEndTimeCombinations.get(bySource.get(dataSet)).add(dataSet);
				}

				for (String date : startEndTimeCombinations.keySet()) {
					String dateFormatted = date;
					// if (!dateFormatted.endsWith("]"))
					// dateFormatted = dateFormatted + "]";
					// dateFormatted = dateFormatted.replace(",]", "]");

					List<String> sources = new ArrayList<String>();
					for (DataSet dataSet : startEndTimeCombinations.get(date))
						sources.add(dataSet.getId());
					Collections.sort(sources);

					dateFormatted = dateFormatted.replace("@st", "-").replace("@et", "-");

					String line = StringUtils.join(sources, ", ") + ";" + dateFormatted;
					writerAllData.write("\t" + line);
				}

				writerAllData.write("\n");

				if (i == 100)
					break;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writerAllData.close();
			for (PrintWriter writer : writerSheets)
				writer.close();
		}

	}

	private String createLabel(Event event) {

		Map<Language, Set<String>> labels = this.eventLabels.get(event);
		for (Language language : this.languagesByPrio) {
			if (labels.containsKey(language)) {
				List<String> languageLabels = new ArrayList<String>();
				languageLabels.addAll(labels.get(language));
				return StringUtils.join(languageLabels, " / ");
			}
		}

		return null;
	}

	private void filterEvents() {

		for (Iterator<String> it = this.events.keySet().iterator(); it.hasNext();) {
			String eventId = it.next();
			Event event = this.events.get(eventId);

			if (compare) {
				// Set<DataSet> dataSets = new HashSet<DataSet>();
				//
				// for (Date date : event.getStartTimesWithDataSets().keySet())
				// dataSets.addAll(event.getStartTimesWithDataSets().get(date));
				// for (Date date : event.getEndTimesWithDataSets().keySet())
				// dataSets.addAll(event.getEndTimesWithDataSets().get(date));

				if (event.getStartTimesWithDataSets().size() + event.getEndTimesWithDataSets().size() <= 2)
					it.remove();
			} else {
				if (event.getStartTimesWithDataSets().isEmpty() || event.getEndTimesWithDataSets().isEmpty())
					it.remove();
			}
		}

		System.out.println("Filtered events: " + this.events.size());

	}

	private void loadEvents() {

		System.out.println("loadEvents");

		int i = 0;
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			while (it.hasNext()) {
				String line = it.nextLine();

				if (i % 100000 == 0)
					System.out.println("Line " + i);
				i += 1;

				if (!line.startsWith("<event"))
					continue;

				if (line.contains("sem:hasBeginTimeStamp") || line.contains("sem:hasEndTimeStamp")) {
					String[] parts = line.split(" ");
					Event event = getEvent(parts[0]);
					String source = parts[3];
					String date = parts[2];
					try {
						if (line.contains("sem:hasBeginTimeStamp"))
							event.addStartTime(TimeTransformer.generateEarliestTimeFromXsd(date), getDataSet(source));
						else
							event.addEndTime(TimeTransformer.generateLatestTimeFromXsd(date), getDataSet(source));
					} catch (ParseException e) {
						continue;
						// e.printStackTrace();
					} catch (NumberFormatException e) {
						continue;
					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

		loadLabels();

		System.out.println("Loaded events: " + this.events.size());

	}

	private void loadLabels() {

		System.out.println("Load labels");

		int i = 0;
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
			while (it.hasNext()) {
				String line = it.nextLine();

				if (i % 100000 == 0)
					System.out.println("Line " + i);
				i += 1;

				if (!line.contains("rdfs:label"))
					continue;

				String eventId = line.substring(0, line.indexOf(" "));
				if (!this.events.containsKey(eventId))
					continue;

				line = line.substring(line.indexOf(" ") + 1);
				line = line.substring(line.indexOf(" ") + 1);

				String label = line.substring(0, line.indexOf(" eventKG-g:"));

				Event event = this.events.get(eventId);
				Language language = Language.valueOf(label.substring(label.length() - 2).toUpperCase());

				label = label.substring(1, label.indexOf("@") - 1);

				if (!eventLabels.containsKey(event))
					eventLabels.put(event, new HashMap<Language, Set<String>>());
				if (!eventLabels.get(event).containsKey(language))
					eventLabels.get(event).put(language, new HashSet<String>());

				eventLabels.get(event).get(language).add(label);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			LineIterator.closeQuietly(it);
		}

	}

	private DataSet getDataSet(String sourceId) {
		sourceId = sourceId.replace("eventKG-g:", "");
		DataSet dataSet = this.dataSets.get(sourceId);

		if (dataSet != null)
			return dataSet;

		System.out.println("New dataSet: " + sourceId);

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

	private Event getEvent(String eventId) {

		Event event = this.events.get(eventId);

		if (event != null)
			return event;

		event = new Event();
		event.setId(eventId);

		this.events.put(eventId, event);

		return event;
	}

}
