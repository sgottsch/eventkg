package de.l3s.eventkg.pipeline;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.ConnectedEntitiesLoader;
import de.l3s.eventkg.integration.DataCollector;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriterMode;
import de.l3s.eventkg.integration.EventKGDBCreatorFromCurrentVersion;
import de.l3s.eventkg.integration.EventKGDBCreatorFromPreviousVersion;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.collection.EventAndTemporalRelationsCollector;
import de.l3s.eventkg.integration.collection.EventDependenciesCollector;
import de.l3s.eventkg.integration.collection.LiteralRelationsCollector;
import de.l3s.eventkg.integration.collection.LocationsCollector;
import de.l3s.eventkg.integration.collection.PositionsCollector;
import de.l3s.eventkg.integration.collection.TimesCollector;
import de.l3s.eventkg.integration.collection.TypesCollector;
import de.l3s.eventkg.integration.db.DBInserter;
import de.l3s.eventkg.integration.integrator.LiteralRelationsIntegrator;
import de.l3s.eventkg.integration.integrator.RelationsIntegrator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.output.DataStoreWriter;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.source.currentevents.CurrentEventsRelationsExtraction;
import de.l3s.eventkg.source.currentevents.EventsFromFileExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaAllLocationsLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaDBOEventsLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaEventLocationsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaEventRelationsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaPartOfLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaPositionsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaTimesExtractor;
import de.l3s.eventkg.source.wikidata.WikidataEventSeriesEditionsFromFileFinder;
import de.l3s.eventkg.source.wikidata.WikidataEventSeriesFromFileFinder;
import de.l3s.eventkg.source.wikidata.WikidataEventsFromFileFinder;
import de.l3s.eventkg.source.wikidata.WikidataExtractionWithEventPages;
import de.l3s.eventkg.source.wikidata.WikidataExtractionWithoutEventPages;
import de.l3s.eventkg.source.wikipedia.EventFirstSentencesWriter;
import de.l3s.eventkg.source.wikipedia.LabelsAndDescriptionsExtractor;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.source.wikipedia.WikipediaCoMentionsExtractor;
import de.l3s.eventkg.source.wikipedia.WikipediaEventsByCategoryNameLoader;
import de.l3s.eventkg.source.wikipedia.WikipediaLinkCountsExtractor;
import de.l3s.eventkg.source.yago.YAGOEventLocationsExtractor;
import de.l3s.eventkg.source.yago.YAGOEventRelationsExtractor;
import de.l3s.eventkg.source.yago.YAGOExistenceTimeExtractor;
import de.l3s.eventkg.source.yago.YAGOIDExtractor;
import de.l3s.eventkg.source.yago.YAGOPositionsExtractor;
import de.l3s.eventkg.textual_events.TextualEventsExtractor;
import de.l3s.eventkg.util.MemoryStatsUtil;

public class Pipeline {

	private List<Language> languages;
	private AllEventPagesDataSet allEventPagesDataSet;

	public static void main(String[] args) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		System.out.println(
				"Run EventKG pipeline :" + StringUtils.join(args, " ") + " (" + dateFormat.format(new Date()) + ")");

		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		Set<Integer> steps = new HashSet<Integer>();
		if (args.length > 0) {
			String arg1 = args[1];
			for (String stepString : arg1.split(",")) {
				steps.add(Integer.valueOf(stepString));
			}
		}

		Pipeline pipeline = new Pipeline(languages);
		Pipeline.initDataSets(languages);

		if (steps.contains(1)) {
			System.out.println("Step 1: Download files.");
			pipeline.download();
		} else
			System.out.println("Skip step 1: Download files.");

		// WikiWords can be initiated after moving meta files.
		WikiWords.getInstance().init(languages);

		if (steps.contains(2)) {
			System.out.println("Step 2: Start extraction -> Find event pages and extract relations.");
			pipeline.pipelineStep2();
		} else
			System.out.println("Skip step 2: Start extraction -> Find event pages and extract relations.");

		if (steps.contains(3)) {
			System.out.println("Step 3: Integration step 1.");
			pipeline.pipelineStep3();
		} else
			System.out.println("Skip step 3: Integration step 1.");

		if (steps.contains(4)) {
			System.out.println(
					"Step 4: Continue extraction -> Extract relations between events and entities with existence times.");
			pipeline.pipelineStep4();
		} else
			System.out.println("Skip step 4: Continue extraction -> Extract relations between events.");

		// if (steps.contains(5)) {
		// System.out.println("Step 5: Integration step 2.");
		// pipeline.pipelineStep5();
		// } else
		// System.out.println("Skip step 5: Integration step 2.");
		//
		// if (steps.contains(6)) {
		// System.out.println("Step 6: Type extraction step 2.");
		// pipeline.pipelineStep6();
		// } else
		// System.out.println("Skip step 6: Type extraction step 2.");

		if (steps.contains(5)) {
			System.out.println("Step 5: Write output (entities and events).");
			pipeline.pipelineStep5();
		} else
			System.out.println("Skip step 5: Write output (entities and events).");

		if (steps.contains(6)) {
			System.out.println("Step 6: Write output (pre-defined relations - times, locations,...).");
			pipeline.pipelineStep6();
		} else
			System.out.println("Skip step 6: Write output (pre-defined relations - times, locations,...).");

		if (steps.contains(7)) {
			System.out.println("Step 7: Write output (event and entity relations).");
			pipeline.pipelineStep7();
		} else
			System.out.println("Skip step 7: Write output (event and entity relations).");

		if (steps.contains(8)) {
			System.out.println("Step 8: Write output (text events).");
			pipeline.pipelineStep8();
		} else
			System.out.println("Skip step 8: Write output (text events).");

		if (steps.contains(9)) {
			System.out.println("Step 9: Write output (links).");
			pipeline.pipelineStep9(true);
		} else
			System.out.println("Skip step 9: Write output (links).");

		if (steps.contains(10)) {
			System.out.println("Step 10: Write output (co-mentions).");
			pipeline.pipelineStep9(false);
		} else
			System.out.println("Skip step 10: Write output (co-mentions).");

	}

	public Pipeline(List<Language> languages) {
		this.languages = languages;
	}

	private void download() {
		RawDataDownLoader downloader = new RawDataDownLoader(languages);
		downloader.createFolders();
		downloader.copyMetaFiles();
		downloader.downloadFiles();
		System.out.println("Finished download files.");
	}

	private void pipelineStep2() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		// Extraction from raw data

		// WCE
		extractors.add(new EventsFromFileExtractor(languages));

		// YAGO
		extractors.add(new YAGOExistenceTimeExtractor(languages));
		extractors.add(new YAGOEventLocationsExtractor(languages));
		extractors.add(new YAGOPositionsExtractor(languages));

		// dbPedia
		extractors.add(new DBpediaDBOEventsLoader(languages));
		extractors.add(new DBpediaEventLocationsExtractor(languages));
		extractors.add(new DBpediaTimesExtractor(languages));
		extractors.add(new DBpediaPartOfLoader(languages));
		extractors.add(new DBpediaPositionsExtractor(languages));

		// Wikipedia
		extractors.add(new WikipediaEventsByCategoryNameLoader(languages));

		// Wikidata
		extractors.add(new WikidataExtractionWithoutEventPages(languages));

		for (Extractor extractor : extractors) {
			extractor.printInformation();
			extractor.run();
		}
	}

	private void pipelineStep3() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		extractors.add(new WikidataEventsFromFileFinder(languages));
		extractors.add(new WikidataEventSeriesFromFileFinder(languages));
		extractors.add(new DBpediaAllLocationsLoader(languages));
		extractors.add(new DBInserter(languages));
		// First step of integration
		extractors.add(new DataCollector(languages));

		for (Extractor extractor : extractors) {
			extractor.printInformation();
			extractor.run();
		}
	}

	private void pipelineStep4() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		// Collect relations from/to events
		extractors.add(new DBpediaEventRelationsExtractor(languages, getAllEventPagesDataSet(true)));
		extractors.add(new CurrentEventsRelationsExtraction(languages, getAllEventPagesDataSet(true)));
		extractors.add(new YAGOEventRelationsExtractor(languages, getAllEventPagesDataSet(true)));
		extractors.add(new WikidataExtractionWithEventPages(languages, getAllEventPagesDataSet(true)));

		for (Extractor extractor : extractors) {
			extractor.printInformation();
			extractor.run();
		}

	}

	private void pipelineStep5() {

		// Event and entity labels and IDs
		System.out.println("Pipeline step 5: Event and entity labels and IDs.");

		List<Extractor> extractors = new ArrayList<Extractor>();
		WikidataIdMappings wikidataIdMappings = getAllEventPagesDataSet(true).getWikidataIdMappings();

		extractors.add(new EventKGDBCreatorFromPreviousVersion(languages));
		extractors.add(new WikidataEventSeriesEditionsFromFileFinder(languages));
		extractors.add(new YAGOIDExtractor(languages, wikidataIdMappings));
		extractors.add(new LabelsAndDescriptionsExtractor(languages, wikidataIdMappings));

		for (Extractor extractor : extractors) {
			extractor.printInformation();
			extractor.run();
			MemoryStatsUtil.printMemoryStats();
		}

		TriplesWriter triplesWriter = new TriplesWriter(true);
		DataStoreWriter outputWriter = new DataStoreWriter(languages, allEventPagesDataSet,
				DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION, triplesWriter);

		try {
			outputWriter.writeNoRelations();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			triplesWriter.close();
		}

		new EventKGDBCreatorFromCurrentVersion(languages).run();

		System.out.println("Done.");
	}

	private void pipelineStep6() {

		System.out.println("Pipeline step 6: Pre-defined entity and event relations (times, locations, types, ...).");

		TriplesWriter triplesWriter = new TriplesWriter(false);
		WikidataIdMappings wikidataIdMappings = getAllEventPagesDataSet(true).getWikidataIdMappings();

		List<Extractor> extractors = new ArrayList<Extractor>();

		extractors.add(new TimesCollector(languages, wikidataIdMappings, triplesWriter));
		extractors.add(new LocationsCollector(languages, wikidataIdMappings, triplesWriter));
		extractors.add(new PositionsCollector(languages, wikidataIdMappings, triplesWriter));
		extractors.add(new EventDependenciesCollector(languages, wikidataIdMappings, triplesWriter));
		extractors.add(new TypesCollector(languages, wikidataIdMappings, triplesWriter));
		extractors.add(new EventFirstSentencesWriter(languages, wikidataIdMappings, triplesWriter));

		try {
			for (Extractor extractor : extractors) {
				extractor.printInformation();
				extractor.run();
				extractor = null;
				MemoryStatsUtil.printMemoryStats();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			triplesWriter.close();
		}

		System.out.println("Done.");
	}

	private void pipelineStep7() {

		System.out.println("Pipeline step 7: Event and entity relations.");

		List<Extractor> extractors = new ArrayList<Extractor>();
		TriplesWriter triplesWriter = new TriplesWriter(false);

		WikidataIdMappings wikidataIdMappings = getAllEventPagesDataSet(true).getWikidataIdMappings();

		extractors.add(new LiteralRelationsCollector(languages, wikidataIdMappings));
		extractors.add(new LiteralRelationsIntegrator(languages, triplesWriter));

		extractors.add(new EventAndTemporalRelationsCollector(languages, wikidataIdMappings));
		extractors.add(new RelationsIntegrator(languages, triplesWriter));

		MemoryStatsUtil.printMemoryStats();

		try {
			for (Extractor extractor : extractors) {
				extractor.printInformation();
				extractor.run();
				MemoryStatsUtil.printMemoryStats();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			triplesWriter.close();
		}

		System.out.println("Done.");
	}

	private void pipelineStep8() {

		System.out.println("Pipeline step 8: Text events.");

		List<Extractor> extractors = new ArrayList<Extractor>();
		TriplesWriter triplesWriter = new TriplesWriter(false);
		WikidataIdMappings wikidataIdMappings = getAllEventPagesDataSet(true).getWikidataIdMappings();

		extractors.add(new TextualEventsExtractor(languages, wikidataIdMappings, triplesWriter));

		MemoryStatsUtil.printMemoryStats();
		try {
			for (Extractor extractor : extractors) {
				extractor.printInformation();
				extractor.run();
				MemoryStatsUtil.printMemoryStats();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			triplesWriter.close();
		}

		System.out.println("Done.");
	}

	private void pipelineStep9(boolean counts) {

		System.out.println("Pipeline step 9: Links.");

		List<Extractor> extractors = new ArrayList<Extractor>();
		TriplesWriter triplesWriter = new TriplesWriter(false);

		WikidataIdMappings wikidataIdMappings = getAllEventPagesDataSet(true).getWikidataIdMappings();

		MemoryStatsUtil.printMemoryStats();

		Map<Entity, Set<Entity>> connectedEntities = ConnectedEntitiesLoader.loadConnectedEntities(wikidataIdMappings);

		MemoryStatsUtil.printMemoryStats();

		if (counts)
			extractors.add(
					new WikipediaLinkCountsExtractor(languages, wikidataIdMappings, triplesWriter, connectedEntities));
		else
			extractors.add(
					new WikipediaCoMentionsExtractor(languages, wikidataIdMappings, triplesWriter, connectedEntities));

		MemoryStatsUtil.printMemoryStats();
		try {
			for (Extractor extractor : extractors) {
				extractor.printInformation();
				extractor.run();
				MemoryStatsUtil.printMemoryStats();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			triplesWriter.close();
		}

		connectedEntities = null;

		System.out.println("Done.");
	}

	private AllEventPagesDataSet getAllEventPagesDataSet(boolean loadEntityAndEventInfo) {
		if (allEventPagesDataSet == null) {
			System.out.println("Init AllEventPagesDataSet.");
			this.allEventPagesDataSet = new AllEventPagesDataSet(languages);
			this.allEventPagesDataSet.setLoadEntityAndEventInfo(loadEntityAndEventInfo);
			this.allEventPagesDataSet.init();
		}
		return this.allEventPagesDataSet;
	}

	public static void initDataSets(List<Language> languages) {

		DataSets.getInstance().addDataSetWithoutLanguage(Source.DBPEDIA, "http://dbpedia.org/");

		for (Language language : languages) {
			DataSets.getInstance().addDataSet(language, Source.DBPEDIA,
					"http://" + language.getLanguageLowerCase() + ".dbpedia.org/");
			DataSets.getInstance().addDataSet(language, Source.WIKIPEDIA,
					"https://dumps.wikimedia.org/" + language.getLanguageLowerCase() + "wiki/");
		}

		DataSets.getInstance().addDataSetWithoutLanguage(Source.WIKIDATA,
				"https://dumps.wikimedia.org/wikidatawiki/entities/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.YAGO,
				"https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/downloads/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WCE, "http://wikitimes.l3s.de/Resource.jsp");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, Config.getValue("uri"));
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2,
		// "http://eventkg.l3s.uni-hannover.de/");
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_LOC,
		// "http://eventkg.l3s.uni-hannover.de/");
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_LOC_2,
		// "http://eventkg.l3s.uni-hannover.de/");

		PrefixList.getInstance().init(languages);

		// set dates of data sets (later needed for the graphs.ttl file)
		SimpleDateFormat configDateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat configDateFormatDBpedia = new SimpleDateFormat("yyyy.MM.dd");

		for (Language language : languages) {

			// Wikipedia
			String wikiName = language.getWiki();
			String dumpDate = Config.getValue(wikiName);
			try {
				DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA).setDate(configDateFormat.parse(dumpDate));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}

			// DBpedia
			String dumpDateDbpedia = Config.getValue("dbpedia");
			try {
				DataSets.getInstance().getDataSet(language, Source.DBPEDIA)
						.setDate(configDateFormatDBpedia.parse(dumpDateDbpedia));
			} catch (ParseException e1) {
				e1.printStackTrace();
			}

		}

		// Wikidata
		String dumpDateWikidata = Config.getValue("wikidata");
		try {
			DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA)
					.setDate(configDateFormat.parse(dumpDateWikidata));
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		// TODO: It is quite unclear to find out the YAGO date. It could also
		// change in future.
		try {
			DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO).setDate(configDateFormat.parse("20170701"));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// WCE: Current date.
		DataSets.getInstance().getDataSet(Language.EN, Source.WCE).setDate(Calendar.getInstance().getTime());

	}
}
