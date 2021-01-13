package de.l3s.eventkg.pipeline.output;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.DataStoreWriterMode;
import de.l3s.eventkg.integration.EntityIdGenerator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.FileType;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.URLUtil;

public class DataStoreWriter {

	private static final DateFormat CONFIG_DBPEDIA_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
	private static final DateFormat CONFIG_WIKIPEDIA_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	private static final DateFormat CONFIG_WIKIDATA_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private DataStore dataStore;
	private DataSets dataSets;

	private DataStoreWriterMode dataStoreWriterMode;

	private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat(
			"\"yyyy-MM-dd\"'^^<" + PrefixEnum.XSD.getUrlPrefix() + "date>'");

	private PrefixList prefixList;

	private List<Language> languages;

	private EntityIdGenerator idGenerator;

	private Prefix basePrefix;

	private AllEventPagesDataSet allEventPagesDataSet;

	private TriplesWriter triplesWriter;

	public static void main(String[] args) {
		System.out.println(OUTPUT_DATE_FORMAT.format(new Date()));
	}

	public DataStoreWriter(List<Language> languages, DataStoreWriterMode dataStoreWriterMode,
			TriplesWriter dataStoreWriter) {
		this(languages, null, dataStoreWriterMode, dataStoreWriter);
	}

	public DataStoreWriter(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet,
			DataStoreWriterMode dataStoreWriterMode, TriplesWriter dataStoreWriter) {
		this.dataStore = DataStore.getInstance();
		this.dataSets = DataSets.getInstance();
		this.languages = languages;
		this.allEventPagesDataSet = allEventPagesDataSet;
		this.dataStoreWriterMode = dataStoreWriterMode;
		this.triplesWriter = dataStoreWriter;
	}

	public void writeNoRelations() {
		System.out.println("writeNoRelations");
		init();
		System.out.println("Write meta files.");
		writeMetaFiles();
		System.out.println("writeEvents");
		writeEvents();
		System.out.println("writeEntities");
		writeEntities();
		System.out.println(" -> writeNoRelations: done");
	}

	private void writeMetaFiles() {
		copySchemaFile();
		createVoIDFile();
	}

	public void init() {

		prefixList = PrefixList.getInstance();

		if (dataStoreWriterMode == DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION)
			this.idGenerator = new EntityIdGenerator(true);
		else if (dataStoreWriterMode == DataStoreWriterMode.USE_IDS_OF_CURRENT_EVENTKG_VERSION)
			this.idGenerator = new EntityIdGenerator(false);

		this.basePrefix = prefixList.getPrefix(PrefixEnum.EVENT_KG_RESOURCE);
	}

	public void initPrefixes() {
		prefixList = PrefixList.getInstance();
		this.basePrefix = prefixList.getPrefix(PrefixEnum.EVENT_KG_RESOURCE);
	}

	private void copySchemaFile() {

		System.out.println("Copy schema file.");

		PrintWriter writer = null;
		PrintWriter writerLight = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_SCHEMA);
			writerLight = FileLoader.getWriterLight(FileName.ALL_TTL_SCHEMA);

			for (String line : FileLoader.readLines(FileName.ALL_TTL_SCHEMA_INPUT)) {
				writer.write(line + Config.NL);
				writerLight.write(line + Config.NL);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerLight.close();
		}
	}

	public void createVoIDFile() {

		System.out.println("Create VoID file.");

		String currentDate = OUTPUT_DATE_FORMAT.format(new Date());
		RDFWriter writer = triplesWriter.getWriter(RDFWriterName.VOID);

		for (String line : FileLoader.readLines(FileName.ALL_TTL_VOID_INPUT)) {
			line = line.replace("@modification_date@", currentDate);
			try {
				line = line.replace("@wikidata_date@",
						OUTPUT_DATE_FORMAT.format(CONFIG_WIKIDATA_DATE_FORMAT.parse(Config.getValue("wikidata"))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			writer.write(line);
		}

		writer.write("");
		writer.write("# Wikipedias");

		for (Language language : this.languages) {
			String languageCode = language.getLanguageLowerCase();
			try {
				Date wikipediaDate = CONFIG_WIKIPEDIA_DATE_FORMAT.parse(Config.getValue(language.getWiki()));

				triplesWriter.writeDataSetDescription("wikipedia_" + languageCode, language,
						language.getLanguageAdjective() + " Wikipedia", "https://" + languageCode + ".wikipedia.org/",
						wikipediaDate);
			} catch (ParseException e) {
			}

			writer.write("");
		}

		writer.write("# DBpedias");

		for (Language language : this.languages) {
			String languageCode = language.getLanguageLowerCase();
			String description = language.getLanguageAdjective() + " DBpedia";

			String homepage = "http://dbpedia.org/";
			if (language != Language.EN) {
				homepage = "http://\" + languageCode + \".dbpedia.org/";
				if (!URLUtil.urlExists(homepage))
					homepage = null;
			}

			try {
				Date dbPediaDate = CONFIG_DBPEDIA_DATE_FORMAT.parse(Config.getValue("dbpedia"));
				triplesWriter.writeDataSetDescription("dbpedia_" + languageCode, language, description, homepage,
						dbPediaDate);
			} catch (ParseException e) {
			}

			writer.write("");
		}

	}

	private void writeEvents() {

		int eventNo = 0;

		if (dataStoreWriterMode == DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION) {
			eventNo = this.idGenerator.getLastEventNo() + 1;
			System.out.println("Last event number in previous version: " + eventNo);
		}

		// "same as" events / Wikidata events
		for (int wikidataId : this.allEventPagesDataSet.getWikidataIdsOfAllEvents()) {
			Event event = this.allEventPagesDataSet.getEventByNumericWikidataId(wikidataId);
			eventNo = writeEvent(event, eventNo);
		}

		// textual events
		for (Event event : dataStore.getEvents()) {
			eventNo = writeEvent(event, eventNo);
		}

	}

	private int writeEvent(Event event, int eventNo) {

		String eventId = this.idGenerator.getEventID(event);
		if (eventId == null) {
			eventId = "event_" + String.valueOf(eventNo);
			eventNo += 1;
		}
		event.setId(eventId);

		triplesWriter.startInstance();

		triplesWriter.writeBasicTypeTriple(event, prefixList.getPrefix(PrefixEnum.SEM), "Event",
				DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);

		if (event.isRecurring())
			triplesWriter.writeBasicTypeTriple(event, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "EventSeries",
					DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);

		if (event.isRecurrentEventEdition())
			triplesWriter.writeBasicTypeTriple(event, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
					"EventSeriesEdition", DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);

		if (event.getWikidataId() != null)
			triplesWriter.writeBasicSameAsTriple(event, prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY),
					event.getWikidataId(), dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), true);

		if (event.getYagoId() != null)
			triplesWriter.writeBasicSameAsTriple(event, prefixList.getPrefix(PrefixEnum.YAGO), event.getYagoId(),
					dataSets.getDataSetWithoutLanguage(Source.YAGO), true);

		if (event.getOtherUrls() != null) {
			for (DataSet dataSet : event.getOtherUrls().keySet()) {
				for (String otherUrl : event.getOtherUrls().get(dataSet)) {
					triplesWriter.writeBasicExtractedTriple(event, otherUrl, dataSet, true);
				}
			}
		}

		// for (Language language : event.getWikipediaLabels().keySet()) {

		Map<Language, Set<String>> names = new HashMap<Language, Set<String>>();
		for (Language language : this.languages)
			names.put(language, new HashSet<String>());

		if (event.getNumericWikidataId() != null) {
			for (Language language : this.languages) {

				// DBpedia ID = Wikipedia label (with space instead of
				// underscore)
				String wikipediaLabel = this.idGenerator.getWikipediaId(event, language);

				if (wikipediaLabel != null) {

					Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
					triplesWriter.writeBasicSameAsTriple(event, prefix, wikipediaLabel,
							dataSets.getDataSet(language, Source.DBPEDIA), true);

					wikipediaLabel = wikipediaLabel.replace("_", " ");
					triplesWriter.writeBasicLabelTriple(event, wikipediaLabel,
							dataSets.getDataSet(language, Source.WIKIPEDIA), language,
							!names.get(language).contains(wikipediaLabel));
					names.get(language).add(wikipediaLabel);
				}

				// Wikidata labels
				String wikidataLabel = this.idGenerator.getWikidataLabel(event, language);

				if (wikidataLabel != null) {
					wikidataLabel = wikidataLabel.replace("_", " ");
					triplesWriter.writeBasicLabelTriple(event, wikidataLabel,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), language,
							!names.get(language).contains(wikidataLabel));
					names.get(language).add(wikidataLabel);
				}
			}
		}

		for (Alias alias : event.getAliases()) {

			if (names.get(alias.getLanguage()).contains(alias.getLabel()))
				continue;

			Language language = alias.getLanguage();
			triplesWriter.writeBasicAliasTriple(event, alias.getLabel(), alias.getDataSet(), language, true);

			names.get(alias.getLanguage()).add(alias.getLabel());
		}

		for (Description description : event.getDescriptions()) {

			if (names.get(description.getLanguage()).contains(description.getLabel()))
				continue;

			Language language = description.getLanguage();
			triplesWriter.writeBasicDescriptionTriple(event, description.getLabel(), description.getDataSet(), language,
					true);
			names.get(description.getLanguage()).add(description.getLabel());
		}

		triplesWriter.endInstance();

		return eventNo;
	}

	private void writeEntities() {

		// String entityId = "entity_";
		int entityNo = 0;

		if (dataStoreWriterMode == DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION)
			entityNo = this.idGenerator.getLastEntityNo() + 1;

		for (Entity entity : this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataNumericIds()
				.values()) {

			if (entity.isEvent())
				continue;

			triplesWriter.startInstance();

			String entityId = this.idGenerator.getEntityID(entity);
			if (entityId == null) {
				entityId = "entity_" + String.valueOf(entityNo);
				entityNo += 1;
			}
			entity.setId(entityId);

			if (entity.isLocation())
				triplesWriter.writeBasicTypeTriple(entity, prefixList.getPrefix(PrefixEnum.SEM), "Place",
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);
			else
				triplesWriter.writeBasicTypeTriple(entity, prefixList.getPrefix(PrefixEnum.SEM), "Actor",
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);

			triplesWriter.writeBasicSameAsTriple(entity, prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY),
					entity.getWikidataId(), dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), true);

			if (entity.getYagoId() != null)
				triplesWriter.writeBasicSameAsTriple(entity, prefixList.getPrefix(PrefixEnum.YAGO),
						entity.getWikidataId(), dataSets.getDataSetWithoutLanguage(Source.YAGO), true);

			Map<Language, Set<String>> names = new HashMap<Language, Set<String>>();
			for (Language language : this.languages)
				names.put(language, new HashSet<String>());
			for (Language language : this.languages) {

				// DBpedia ID = Wikipedia label (with space instead of
				// underscore)
				String wikipediaLabel = this.idGenerator.getWikipediaId(entity, language);

				if (wikipediaLabel != null) {

					Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
					triplesWriter.writeBasicSameAsTriple(entity, prefix, wikipediaLabel,
							dataSets.getDataSet(language, Source.DBPEDIA), true);

					wikipediaLabel = wikipediaLabel.replace("_", " ");
					triplesWriter.writeBasicLabelTriple(entity, wikipediaLabel,
							dataSets.getDataSet(language, Source.WIKIPEDIA), language,
							!names.get(language).contains(wikipediaLabel));
					names.get(language).add(wikipediaLabel);
				}

				// Wikidata labels
				String wikidataLabel = this.idGenerator.getWikidataLabel(entity, language);

				if (wikidataLabel != null) {
					wikidataLabel = wikidataLabel.replace("_", " ");
					triplesWriter.writeBasicLabelTriple(entity, wikidataLabel,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), language,
							!names.get(language).contains(wikidataLabel));
					names.get(language).add(wikidataLabel);
				}
			}

			triplesWriter.endInstance();
		}
	}

	public List<String> createIntro(List<Prefix> prefixes, PrefixList prefixList, FileType fileType) {

		boolean containsEventKGGraph = false;
		for (Prefix prefix : prefixes) {
			if (prefix.getPrefixEnum() == PrefixEnum.EVENT_KG_GRAPH) {
				containsEventKGGraph = true;
				break;
			}
		}
		if (!containsEventKGGraph)
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH));

		List<String> lines = new ArrayList<String>();

		lines.add("");
		for (Prefix prefix : prefixes) {
			// ignore base relation
			if (prefix.getPrefixEnum() == PrefixEnum.EVENT_KG_RESOURCE)
				continue;

			if (prefix.getPrefixEnum() == PrefixEnum.NO_PREFIX)
				continue;

			lines.add(
					"@prefix" + Config.SEP + prefix.getAbbr() + " <" + prefix.getUrlPrefix() + ">" + Config.SEP + ".");
		}

		lines.add("@base" + Config.SEP + "<" + this.basePrefix.getUrlPrefix() + ">" + Config.SEP + ".");

		lines.add("");

		if (fileType == FileType.NQ)
			lines.clear();

		return lines;
	}

}
