package de.l3s.eventkg.integration.collection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.integrator.TimesIntegrator;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class TimesCollector extends Extractor {

	private List<Language> languages;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter dataStoreWriter;

	public TimesCollector(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("TimesCollector", Source.ALL, "Collects, fuses start and end times into a common graph.", languages);
		this.languages = languages;
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	public void run() {
		loadTimes();
		integrateTimes();
		clear();
	}

	private void clear() {
		for (Entity entity : this.wikidataIdMappings.getEntitiesByWikidataNumericIds().values()) {
			entity.clearTimes();
		}
	}

	private void loadTimes() {

		// "event.setStartTime()" is needed for the matching of textual to named
		// events. To this end, collect times by trust of the source. The last
		// one should be the most trustworthy and overwrite the others.

		collectTimesDBpedia();
		collectTimesYAGO();
		collectTimesWikidata();

		writeEntitiesWithExistenceTimes();
	}

	private void integrateTimes() {
		TimesIntegrator timesIntegrator = new TimesIntegrator(languages, dataStoreWriter, wikidataIdMappings);
		timesIntegrator.run();
	}

	private void collectTimesDBpedia() {

		System.out.println("collectTimesDBpedia");

		for (Language language : this.languages) {

			BufferedReader br = null;
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_TIMES, language);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String line;
			try {
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipediaLabel = parts[0];
					String timeString = parts[2];

					TimeSymbol type = TimeSymbol.fromString(parts[3]);

					// event: happening time. entity: existence time
					Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);

					if (entity == null)
						continue;

					DateWithGranularity date;
					try {
						date = TimeTransformer.generateTimeForDBpedia(timeString);

						if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {
							// if (entity.isEvent()) {
							// entity.setStartTime(date);
							entity.addStartTime(date, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
							// }

							// this.startTimes.add(new StartTime(entity,
							// DataSets.getInstance().getDataSet(language,
							// Source.DBPEDIA), date));
						}
						if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {

							// if (entity.isEvent()) {
							// event.setEndTime(date);
							entity.addEndTime(date, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
							// }

							// this.endTimes.add(new EndTime(entity,
							// DataSets.getInstance().getDataSet(language,
							// Source.DBPEDIA), date));
						}

					} catch (ParseException e) {
						System.out.println("Error: Cannot parse " + timeString + ". Ignore.");
						// e.printStackTrace();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void collectTimesWikidata() {
		System.out.println("collectTimesWikidata");
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				String entityWikidataId = parts[0];

				// event: happening time. entity: existence time
				Entity entity = this.wikidataIdMappings.getEntityByWikidataId(entityWikidataId);

				if (entity == null)
					continue;

				String propertyWikidataId = parts[1];
				String timeString = parts[2];

				TimeSymbol type = wikidataIdMappings.getWikidataTemporalPropertyTypeById(propertyWikidataId);

				try {

					if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {

						DateWithGranularity dateEarliest = TimeTransformer.generateEarliestTimeForWikidata(timeString);

						if (dateEarliest != null) {

							// if (entity.isEvent()) {
							// event.setStartTime(dateEarliest);
							entity.addStartTime(dateEarliest,
									DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
							// }

							// this.startTimes.add(new StartTime(entity,
							// DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA),
							// dateEarliest));

						}
					}
					if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {
						DateWithGranularity dateLatest = TimeTransformer.generateLatestTimeForWikidata(timeString);

						if (dateLatest != null) {

							// if (entity.isEvent()) {
							// event.setEndTime(dateLatest);
							entity.addEndTime(dateLatest,
									DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
							// }

							// this.endTimes.add(new EndTime(entity,
							// DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA),
							// dateLatest));
						}
					}

				} catch (ParseException e) {
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void collectTimesYAGO() {

		System.out.println("collectTimesYAGO");

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_EXISTENCE_TIMES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				YAGOLabelExtractor yagoLabelExtractor = new YAGOLabelExtractor(parts[0], this.languages);
				yagoLabelExtractor.extractLabel();
				if (!yagoLabelExtractor.isValid())
					continue;

				Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(yagoLabelExtractor.getLanguage(),
						yagoLabelExtractor.getWikipediaLabel());

				if (entity == null)
					continue;

				if (entity.isEvent()) {
					// Events in YAGO very often have wrong times when the
					// "wasDestroyedOnDate" property is used. Ignore them.
					if (parts[1].equals("<wasDestroyedOnDate>"))
						continue;
				}

				// System.out.println("Event: " + event + " -");
				// if(event!=null)
				// System.out.println("not null");

				String timeString = parts[2];
				TimeSymbol type = TimeSymbol.fromString(parts[3]);

				try {
					DateWithGranularity date1 = TimeTransformer.generateEarliestTimeFromXsd(timeString);
					DateWithGranularity date1L = TimeTransformer.generateLatestTimeFromXsd(timeString);

					if (date1 != null && (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME)) {

						// if (entity.isEvent()) {
						// event.setStartTime(date1);
						entity.addStartTime(date1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
						// }

						// this.startTimes.add(new StartTime(entity,
						// DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
						// date1));
					}

					if (date1L != null && (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME)) {

						// if (entity.isEvent()) {
						// event.setEndTime(date1L);
						entity.addEndTime(date1L, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
						// }

						// this.endTimes.add(new EndTime(entity,
						// DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
						// date1L));
					}

				} catch (ParseException e) {
					System.err.println("Error with line: " + line);
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeEntitiesWithExistenceTimes() {

		PrintWriter entitiesWithExistenceTimesWriter;
		try {
			entitiesWithExistenceTimesWriter = FileLoader.getWriter(FileName.ALL_ENTITIES_WITH_EXISTENCE_TIMES);

			for (Entity entity : this.wikidataIdMappings.getEntitiesByWikidataNumericIds().values()) {
				if (!entity.getStartTimesWithDataSets().isEmpty() || !entity.getEndTimesWithDataSets().isEmpty()) {
					entitiesWithExistenceTimesWriter.println(entity.getNumericWikidataId());
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
