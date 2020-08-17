package de.l3s.eventkg.integration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.FileType;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.LiteralDataType;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.integration.model.relation.SubProperty;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.MemoryStatsUtil;
import de.l3s.eventkg.util.URLUtil;

public class DataStoreWriter {

	// .nq (N-QUADS) and .nt do not allow the use of directives, so no prefix
	// definitions may be used. It is, however, still possible to import them in
	// OpenLink Virtuoso.
	public static final boolean ALLOW_DIRECTIVES = false;

	private static final int NUMBER_OF_LINES_IN_PREVIEW = 50;

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

	// private Set<String> propertiesUsedInNamedEventRelations = new
	// HashSet<String>();
	private int relationNo;
	private EntityIdGenerator idGenerator;

	private Prefix basePrefix;

	private AllEventPagesDataSet allEventPagesDataSet;

	public static void main(String[] args) {

		System.out.println(OUTPUT_DATE_FORMAT.format(new Date()));
	}

	public DataStoreWriter(List<Language> languages, DataStoreWriterMode dataStoreWriterMode) {
		this(languages, null, dataStoreWriterMode);
	}

	public DataStoreWriter(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet,
			DataStoreWriterMode dataStoreWriterMode) {
		this.dataStore = DataStore.getInstance();
		this.dataSets = DataSets.getInstance();
		this.languages = languages;
		this.allEventPagesDataSet = allEventPagesDataSet;
		this.dataStoreWriterMode = dataStoreWriterMode;
	}

	public void writeNoRelations() {
		init();
		System.out.println("Write meta files.");
		writeMetaFiles();
		System.out.println("writeEvents");
		writeEvents();
		System.out.println("writeEntities");
		writeEntities();
		System.out.println("writeBaseRelations");
		writeBaseRelations();
	}

	public void writeRelations() {

		MemoryStatsUtil.printMemoryStats();

		init();

		System.out.println("writeNamedEventRelations");
		writeNamedEventRelations();
		System.out.println("writeOtherRelations");
		writeEntityRelations();
		System.out.println("writeLiteralsRelations");
		writeLiteralsRelations();
		System.out.println("writePropertyLabels");
		writePropertyLabels();
	}

	public void writeLinkRelations() {
		System.out.println("Init mapping.");
		init();
		// System.out.println("Init IDs.");
		// initIds();

		System.out.println("Write link relations");
		writeLinkRelationsToFile();
	}

	private void writeMetaFiles() {
		writeDataSetDescriptions();
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

	private void writePropertyLabels() {

		FileType fileType = FileType.NQ;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDFS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_PROPERTY));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (PropertyLabel propertyLabel : dataStore.getPropertyLabels()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, propertyLabel.getPrefix(), propertyLabel.getProperty(),
						prefixList.getPrefix(PrefixEnum.RDFS), "label", null, propertyLabel.getLabel(), true,
						propertyLabel.getDataSet(), propertyLabel.getLanguage(), FileType.NQ);
				// if
				// (propertiesUsedInNamedEventRelations.contains(propertyLabel.getProperty()))
				// writeTriple(writer, writerPreview, lineNo,
				// propertyLabel.getPrefix(), propertyLabel.getProperty(),
				// prefixList.getPrefix(PrefixEnum.RDF), "type",
				// prefixList.getPrefix(PrefixEnum.SEM),
				// "RoleType", true, propertyLabel.getDataSet(),
				// propertyLabel.getLanguage(), fileType);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}
	}

	public void writeDataSetDescriptions() {

		init();
		System.out.println(this.prefixList);
		FileType fileType = FileType.TTL;

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_DATASETS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.VOID));
			prefixes.add(prefixList.getPrefix(PrefixEnum.FOAF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DCTERMS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.XSD));
			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
			}

			for (DataSet dataSet : dataSets.getAllDataSets()) {
				writeTriple(writer, null, null, prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
						prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.FOAF), "Dataset",
						false, null, fileType);
				writeTriple(writer, null, null, prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
						prefixList.getPrefix(PrefixEnum.FOAF), "homepage", null, dataSet.getUrl(), false, null,
						fileType);
				if (dataSet.getDate() != null)
					writeTriple(writer, null, null, prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
							prefixList.getPrefix(PrefixEnum.DCTERMS), "created", null,
							OUTPUT_DATE_FORMAT.format(dataSet.getDate()), false, null, fileType);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void copySchemaFile() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_SCHEMA);

			for (String line : FileLoader.readLines(FileName.ALL_TTL_SCHEMA_INPUT)) {
				writer.write(line + Config.NL);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	public void createVoIDFile() {

		String currentDate = OUTPUT_DATE_FORMAT.format(new Date());

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_VOID);

			for (String line : FileLoader.readLines(FileName.ALL_TTL_VOID_INPUT)) {
				line = line.replace("@modification_date@", currentDate);
				try {
					line = line.replace("@wikidata_date@",
							OUTPUT_DATE_FORMAT.format(CONFIG_WIKIDATA_DATE_FORMAT.parse(Config.getValue("wikidata"))));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				writer.write(line + Config.NL);
			}

			writer.write("# Wikipedias" + Config.NL);

			for (Language language : this.languages) {
				String languageCode = language.getLanguageLowerCase();
				writer.write(
						"eventKG-g:event_kg dcterms:source eventKG-g:wikipedia_" + languageCode + " ." + Config.NL);
				writer.write("eventKG-g:wikipedia_" + languageCode + " rdf:type void:Dataset ." + Config.NL);
				writer.write("eventKG-g:wikipedia_" + languageCode + " dcterms:language \"" + languageCode + "\" ."
						+ Config.NL);
				writer.write("eventKG-g:wikipedia_" + languageCode + " dcterms:description \""
						+ language.getLanguageAdjective() + " Wikipedia\" ." + Config.NL);
				writer.write("eventKG-g:wikipedia_" + languageCode + " foaf:homepage <https://" + languageCode
						+ ".wikipedia.org/>  ." + Config.NL);

				try {
					Date wikipediaDate = CONFIG_WIKIPEDIA_DATE_FORMAT.parse(Config.getValue(language.getWiki()));
					writer.write("eventKG-g:wikipedia_" + languageCode + " dcterms:modified "
							+ OUTPUT_DATE_FORMAT.format(wikipediaDate) + " ." + Config.NL);
				} catch (ParseException e) {
				}

				writer.write(Config.NL);
			}

			writer.write("# DBpedias" + Config.NL);

			for (Language language : this.languages) {
				String languageCode = language.getLanguageLowerCase();
				writer.write("eventKG-g:event_kg dcterms:source eventKG-g:dbpedia_" + languageCode + " ." + Config.NL);
				writer.write("eventKG-g:dbpedia_" + languageCode + " rdf:type void:Dataset ." + Config.NL);
				writer.write("eventKG-g:dbpedia_" + languageCode + " dcterms:language \"" + languageCode + "\" ."
						+ Config.NL);
				writer.write("eventKG-g:dbpedia_" + languageCode + " dcterms:description \""
						+ language.getLanguageAdjective() + " DBpedia\" ." + Config.NL);

				if (language == Language.EN) {
					writer.write("eventKG-g:dbpedia_" + languageCode + " foaf:homepage <http://dbpedia.org/>  ."
							+ Config.NL);
				} else {
					String homepage = "http://" + languageCode + ".dbpedia.org/";
					if (URLUtil.urlExists(homepage))
						writer.write("eventKG-g:dbpedia_" + languageCode + " foaf:homepage <" + homepage + ">  ."
								+ Config.NL);
				}

				try {
					Date dbPediaDate = CONFIG_DBPEDIA_DATE_FORMAT.parse(Config.getValue("dbpedia"));
					writer.write("eventKG-g:dbpedia_" + languageCode + " dcterms:modified "
							+ OUTPUT_DATE_FORMAT.format(dbPediaDate) + " ." + Config.NL);
				} catch (ParseException e) {
				}

				writer.write(Config.NL);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeEvents() {

		FileType fileType = FileType.NQ;

		int eventNo = 0;

		if (dataStoreWriterMode == DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION)
			eventNo = this.idGenerator.getLastEventNo() + 1;

		// Map<Event, String> descriptionMap = createDescriptionMap();

		PrintWriter writer = null;
		PrintWriter writerPreview = null;

		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDFS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DCTERMS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY));
			prefixes.add(prefixList.getPrefix(PrefixEnum.SEM));
			prefixes.add(prefixList.getPrefix(PrefixEnum.OWL));
			prefixes.add(prefixList.getPrefix(PrefixEnum.YAGO));

			for (Language language : this.languages)
				prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language));

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;

			// "same as" events / Wikidata events
			for (int wikidataId : this.allEventPagesDataSet.getWikidataIdsOfAllEvents()) {
				Event event = this.allEventPagesDataSet.getEventByNumericWikidataId(wikidataId);
				int[] res = writeEvent(event, eventNo, lineNo, writer, writerPreview, fileType);
				eventNo = res[0];
				lineNo = res[1];
			}

			lineNo = 0;

			// textual events
			for (Event event : dataStore.getEvents()) {
				int[] res = writeEvent(event, eventNo, lineNo, writer, writerPreview, fileType);
				eventNo = res[0];
				lineNo = res[1];
			}

		} catch (

		FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private int[] writeEvent(Event event, int eventNo, int lineNo, PrintWriter writer, PrintWriter writerPreview,
			FileType fileType) {

		String eventId = this.idGenerator.getEventID(event);
		if (eventId == null) {
			eventId = "event_" + String.valueOf(eventNo);
			eventNo += 1;
		}
		event.setId(eventId);

		lineNo += 1;
		writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(), prefixList.getPrefix(PrefixEnum.RDF),
				"type", prefixList.getPrefix(PrefixEnum.SEM), "Event", false,
				DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), fileType);

		if (event.isRecurring()) {
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
					"EventSeries", false, DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), fileType);
		}

		if (event.isRecurrentEventEdition()) {
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
					"EventSeriesEdition", false, DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG),
					fileType);
		}

		if (event.getWikidataId() != null)
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY),
					event.getWikidataId(), false, dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), fileType);

		if (event.getYagoId() != null)
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefixList.getPrefix(PrefixEnum.YAGO),
					event.getYagoId(), false, dataSets.getDataSetWithoutLanguage(Source.YAGO), fileType);

		if (event.getOtherUrls() != null) {
			for (DataSet dataSet : event.getOtherUrls().keySet()) {
				for (String otherUrl : event.getOtherUrls().get(dataSet)) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
							prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "extractedFrom", null,
							"<" + otherUrl + ">", false, dataSet, fileType);
				}
			}
		}

		// for (Language language : event.getWikipediaLabels().keySet()) {

		if (event.getNumericWikidataId() != null) {
			for (Language language : this.languages) {

				// DBpedia ID = Wikipedia label (with space instead of
				// underscore)
				String wikipediaLabel = this.idGenerator.getWikipediaId(event, language);

				if (wikipediaLabel != null) {
					Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
							prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefix, wikipediaLabel, false,
							dataSets.getDataSet(language, Source.DBPEDIA), fileType);

					writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
							prefixList.getPrefix(PrefixEnum.RDFS), "label", null, wikipediaLabel.replace("_", " "),
							true, dataSets.getDataSet(language, Source.WIKIPEDIA), language, fileType);
				}

				// Wikidata labels
				String wikidataLabel = this.idGenerator.getWikidataLabel(event, language);
				if (wikidataLabel != null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
							prefixList.getPrefix(PrefixEnum.RDFS), "label", null, wikidataLabel, true,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), language, fileType);
				}
			}
		}

		for (Alias alias : event.getAliases()) {
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.DCTERMS), "alternative", null, alias.getLabel(), true,
					alias.getDataSet(), alias.getLanguage(), fileType);
		}

		for (Description description : event.getDescriptions()) {
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.DCTERMS), "description", null, description.getLabel(), true,
					description.getDataSet(), description.getLanguage(), fileType);
		}

		return new int[] { eventNo, lineNo };
	}

	private void writeEntities() {

		FileType fileType = FileType.NQ;

		// String entityId = "entity_";
		int entityNo = 0;

		if (dataStoreWriterMode == DataStoreWriterMode.RE_USE_IDS_OF_PREVIOUS_EVENTKG_VERSION)
			entityNo = this.idGenerator.getLastEntityNo() + 1;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_WITH_TEXTS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_WITH_TEXTS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDFS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DCTERMS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.SEM));
			prefixes.add(prefixList.getPrefix(PrefixEnum.YAGO));
			prefixes.add(prefixList.getPrefix(PrefixEnum.OWL));
			for (Language language : this.languages)
				prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY));

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;

			for (Entity entity : this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataNumericIds()
					.values()) {

				if (entity.isEvent())
					continue;

				lineNo += 1;

				String entityId = this.idGenerator.getEntityID(entity);
				if (entityId == null) {
					entityId = "entity_" + String.valueOf(entityNo);
					entityNo += 1;
				}
				entity.setId(entityId);

				if (entity.isActor())
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.SEM), "Actor",
							false, null, fileType);

				if (entity.isLocation())
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.SEM), "Place",
							false, null, fileType);

				if (!entity.isActor() && !entity.isLocation())
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.SEM), "Core",
							false, null, fileType);

				writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
						prefixList.getPrefix(PrefixEnum.OWL), "sameAs",
						prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY), entity.getWikidataId(), false,
						dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), fileType);

				if (entity.getYagoId() != null)
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
							prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefixList.getPrefix(PrefixEnum.YAGO),
							entity.getYagoId(), false, dataSets.getDataSetWithoutLanguage(Source.YAGO), fileType);

				for (Language language : this.languages) {

					// DBpedia ID = Wikipedia label (with space instead of
					// underscore)
					String wikipediaLabel = this.idGenerator.getWikipediaId(entity, language);
					if (wikipediaLabel != null) {
						Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
						writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
								prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefix, wikipediaLabel, false,
								dataSets.getDataSet(language, Source.DBPEDIA), fileType);

						writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
								prefixList.getPrefix(PrefixEnum.RDFS), "label", null, wikipediaLabel.replace("_", " "),
								true, dataSets.getDataSet(language, Source.WIKIPEDIA), language, fileType);
					}

					// Wikidata labels
					String wikidataLabel = this.idGenerator.getWikidataLabel(entity, language);
					if (wikidataLabel != null) {
						writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
								prefixList.getPrefix(PrefixEnum.RDFS), "label", null, wikidataLabel, true,
								dataSets.getDataSetWithoutLanguage(Source.WIKIDATA), language, fileType);
					}
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeBaseRelations() {

		FileType fileType = FileType.NQ;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.SCHEMA_ORG));
			prefixes.add(prefixList.getPrefix(PrefixEnum.SEM));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY));
			prefixes.add(prefixList.getPrefix(PrefixEnum.XSD));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.TIME));

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			System.out.println("Locations: " + dataStore.getLocations().size());
			for (Location location : dataStore.getLocations()) {

				lineNo += 1;

				if (location.getSubject().getNumericWikidataId() == 171416) {
					System.out.println("Write?");
					System.out.println("1. " + this.idGenerator.getID(location.getSubject()));
					System.out.println("2. " + this.idGenerator.getID(location.getLocation()));
					System.out.println("3. " + location.getLocation().getWikidataId());
					System.out.println("4. " + location.getDataSet().getId());
					System.out.println("");
				}

				writeTriple(writer, writerPreview, lineNo, this.basePrefix,
						this.idGenerator.getID(location.getSubject()), prefixList.getPrefix(PrefixEnum.SEM), "hasPlace",
						this.basePrefix, this.idGenerator.getID(location.getLocation()), false, location.getDataSet(),
						null);
			}

			lineNo = 0;
			for (StartTime startTime : dataStore.getStartTimes()) {

				if (startTime.getStartTime() == null) {
					// TODO: Why?
					System.out.println("Start time is null: " + startTime.getSubject().getId() + ", "
							+ startTime.getSubject().getWikidataId() + ", " + startTime.getDataSet());
					if (startTime.getDataSet() != null)
						System.out.println(" " + startTime.getDataSet().getId());
					continue;
				}

				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, startTime.getSubject().getId(),
						prefixList.getPrefix(PrefixEnum.SEM), "hasBeginTimeStamp", null,
						OUTPUT_DATE_FORMAT.format(startTime.getStartTime().getDate()), false, startTime.getDataSet(),
						null);
				writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix,
						startTime.getSubject().getId(), startTime.getStartTime().getGranularity(), false,
						startTime.getDataSet(), true, fileType);
			}

			lineNo = 0;
			for (EndTime endTime : dataStore.getEndTimes()) {

				if (endTime.getEndTime() == null) {
					// TODO: Why?
					System.out.println("End time is null: " + endTime.getSubject().getId() + ", "
							+ endTime.getSubject().getWikidataId() + ", " + endTime.getDataSet());
					if (endTime.getDataSet() != null)
						System.out.println(" " + endTime.getDataSet().getId());
					continue;
				}

				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, endTime.getSubject().getId(),
						prefixList.getPrefix(PrefixEnum.SEM), "hasEndTimeStamp", null,
						OUTPUT_DATE_FORMAT.format(endTime.getEndTime().getDate()), false, endTime.getDataSet(), null);
				writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix, endTime.getSubject().getId(),
						endTime.getEndTime().getGranularity(), false, endTime.getDataSet(), false, fileType);
			}

			lineNo = 0;
			for (Entity entity : this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataNumericIds()
					.values()) {

				if (entity.isLocation()) {

					// don't write sub locations - it's symmetric to parent
					// location
					// for (Entity subLocation : entity.getSubLocations()) {
					// if (subLocation.getId() == null)
					// System.out.println("S NULL: " +
					// subLocation.getWikidataId() + " / "
					// + subLocation.getWikipediaLabel(Language.EN) + " - " +
					// entity.isEvent());
					// else {
					// lineNo += 1;
					// writeTriple(writer, writerPreview, lineNo,
					// entity.getId(),
					// Prefix.SCHEMA_ORG.getAbbr() + "subLocation",
					// subLocation.getId(), false, null);
					// }
					// }

					for (Entity parentLocation : entity.getParentLocations()) {
						if (parentLocation.getId() == null)
							System.out.println("P NULL: " + parentLocation.getWikidataId() + " / "
									+ parentLocation.getWikipediaLabel(Language.EN) + " - " + entity.isEvent());
						else {
							lineNo += 1;
							writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
									prefixList.getPrefix(PrefixEnum.SCHEMA_ORG), "containedInPlace", this.basePrefix,
									parentLocation.getId(), false, null, fileType);
						}

					}

					if (!entity.getPositions().isEmpty()) {
						lineNo += 1;
						for (Position position : entity.getPositions()) {
							DataSet dataSet = entity.getPositionsWithDataSets().get(position);
							writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
									prefixList.getPrefix(PrefixEnum.SCHEMA_ORG), "latitude", null,
									createLiteral(String.valueOf(position.getLatitude()), LiteralDataType.DOUBLE),
									false, dataSet, fileType);
							writeTriple(writer, writerPreview, lineNo, this.basePrefix, entity.getId(),
									prefixList.getPrefix(PrefixEnum.SCHEMA_ORG), "longitude", null,
									createLiteral(String.valueOf(position.getLongitude()), LiteralDataType.DOUBLE),
									false, dataSet, fileType);
						}
					}

				}
			}

			lineNo = 0;

			for (int wikidataId : this.allEventPagesDataSet.getWikidataIdsOfAllEvents()) {
				Event event = this.allEventPagesDataSet.getEventByNumericWikidataId(wikidataId);
				lineNo = writeEventBaseRelations(event, lineNo, writer, writerPreview, fileType);
			}

			for (Event event : dataStore.getEvents()) {
				lineNo = writeEventBaseRelations(event, lineNo, writer, writerPreview, fileType);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private int writeEventBaseRelations(Event event, int lineNo, PrintWriter writer, PrintWriter writerPreview,
			FileType fileType) {
		int lineNoPlusOne = lineNo + 1;

		for (Event parentEvent : event.getParents()) {
			lineNo += 1;
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, parentEvent.getId(),
					prefixList.getPrefix(PrefixEnum.SEM), "hasSubEvent", this.basePrefix, event.getId(), false, null,
					fileType);
		}

		for (Event nextEvent : event.getNextEvents()) {
			lineNo += 1;
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), "nextEvent", this.basePrefix, nextEvent.getId(),
					false, null, fileType);
		}

		for (Event previousEvent : event.getPreviousEvents()) {
			lineNo += 1;
			writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
					prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), "previousEvent", this.basePrefix,
					previousEvent.getId(), false, null, fileType);
		}

		for (DataSet categoryDataSet : event.getCategories().keySet()) {
			for (Language categoryLanguage : event.getCategories().get(categoryDataSet).keySet()) {
				for (String category : event.getCategories().get(categoryDataSet).get(categoryLanguage)) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
							prefixList.getPrefix(PrefixEnum.DCTERMS), "title", null, createLiteral(category,
									LiteralDataType.LANG_STRING, categoryLanguage.getLanguageLowerCase()),
							false, categoryDataSet, fileType);
				}
			}
		}

		for (DataSet sourceDataSet : event.getSources().keySet()) {
			for (String source : event.getSources().get(sourceDataSet)) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
						prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), "source", null, "<" + source + ">", false,
						sourceDataSet, fileType);
			}
		}

		if (!event.getPositions().isEmpty()) {
			lineNo += 1;
			for (Position position : event.getPositions()) {
				DataSet dataSet = event.getPositionsWithDataSets().get(position);
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
						prefixList.getPrefix(PrefixEnum.SCHEMA_ORG), "latitude", null,
						createLiteral(String.valueOf(position.getLatitude()), LiteralDataType.DOUBLE), false, dataSet,
						fileType);
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, event.getId(),
						prefixList.getPrefix(PrefixEnum.SCHEMA_ORG), "longitude", null,
						createLiteral(String.valueOf(position.getLongitude()), LiteralDataType.DOUBLE), false, dataSet,
						fileType);
			}
		}

		return Math.min(lineNo, lineNoPlusOne);
	}

	private void writeLinkRelationsToFile() {

		FileType fileType = FileType.NQ;

		PrintWriter writerEventLinks = null;
		PrintWriter writerEventLinksPreview = null;
		PrintWriter writerEntityLinks = null;
		PrintWriter writerEntityLinksPreview = null;
		try {
			writerEventLinks = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_LINK_RELATIONS);
			writerEventLinksPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_LINK_RELATIONS_PREVIEW);
			writerEntityLinks = FileLoader.getWriter(FileName.ALL_TTL_ENTITY_LINK_RELATIONS);
			writerEntityLinksPreview = FileLoader.getWriter(FileName.ALL_TTL_ENTITY_LINK_RELATIONS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			// prefixes.add(Prefix.RDF);
			// prefixes.add(Prefix.EVENT_KG);
			prefixes.add(prefixList.getPrefix(PrefixEnum.XSD));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writerEventLinks.write(line + Config.NL);
				writerEventLinksPreview.write(line + Config.NL);
				writerEntityLinks.write(line + Config.NL);
				writerEntityLinksPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			int lineNoEvents = 0;
			int lineNoEntities = 0;

			int svov = 0; // subject event object event
			int snov = 0; // Subject eNtity Object eVent
			int svon = 0;
			int snon = 0;

			for (Set<GenericRelation> relations : dataStore.getLinkRelationsBySubjectAndObjectGroup().values()) {

				GenericRelation exampleRelation = null;
				for (GenericRelation relation : relations) {
					exampleRelation = relation;
					break;
				}

				boolean isEntityRelation = true;

				Entity subject = exampleRelation.getSubject();
				if (subject.isEvent())
					isEntityRelation = false;

				Entity object = exampleRelation.getObject();
				if (object.isEvent())
					isEntityRelation = false;

				if (subject.isEvent() && object.isEvent())
					svov += 1;
				else if (!subject.isEvent() && object.isEvent())
					snov += 1;
				else if (subject.isEvent() && !object.isEvent())
					svon += 1;
				else if (!subject.isEvent() && !object.isEvent())
					snon += 1;

				PrintWriter writer = null;
				PrintWriter writerPreview = null;

				Integer lineNoWriter = null;
				if (isEntityRelation) {
					lineNoWriter = lineNoEntities;
					lineNoEntities += 1;
					writer = writerEntityLinks;
					writerPreview = writerEntityLinksPreview;
				} else {
					lineNoWriter = lineNoEvents;
					lineNoEvents += 1;
					writer = writerEventLinks;
					writerPreview = writerEventLinksPreview;
				}
				lineNo += 1;

				if (subject.getId() == null) {
					String id = this.idGenerator.getID(subject);
					if (id == null)
						continue;
					subject.setId(id);
				}
				if (object.getId() == null) {
					String id = this.idGenerator.getID(object);
					if (id == null)
						continue;
					object.setId(id);
				}

				String relationId = "eventkg_link_relation_" + String.valueOf(lineNo);

				writeTriple(writer, writerPreview, lineNoWriter, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
						"Relation", false, null, fileType);
				writeTriple(writer, writerPreview, lineNoWriter, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "subject", this.basePrefix, subject.getId(), false, null,
						fileType);
				writeTriple(writer, writerPreview, lineNoWriter, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "object", this.basePrefix, object.getId(), false, null,
						fileType);

				for (GenericRelation relation : relations) {
					writeTriple(writer, writerPreview, lineNoWriter, this.basePrefix, relationId, relation.getPrefix(),
							relation.getProperty(), null,
							"\"" + String.valueOf(relation.getWeight().intValue()) + "\"^^<"
									+ prefixList.getPrefix(PrefixEnum.XSD).getUrlPrefix() + "nonNegativeInteger>",
							false, relation.getDataSet(), fileType);
				}
			}

			System.out.println("Subject is event, Object is event:\t" + svov);
			System.out.println("Subject is no event, Object is event:\t" + snov);
			System.out.println("Subject is event, Object is no event:\t" + svon);
			System.out.println("Subject is no event, Object is no event:\t" + snon);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writerEventLinks.close();
			writerEventLinksPreview.close();
			writerEntityLinks.close();
			writerEntityLinksPreview.close();
		}

	}

	private void writeNamedEventRelations() {

		FileType fileType = FileType.NQ;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {

			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_OTHER_RELATIONS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_OTHER_RELATIONS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			// prefixes.add(Prefix.EVENT_KG_SCHEMA);
			// prefixes.add(Prefix.RDF);

			// add all prefixes, because they could all be in the
			// eventKGRelations
			for (Prefix prefix : prefixList.getAllPrefixes())
				prefixes.add(prefix);

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			this.relationNo = 0;

			for (GenericRelation relation : dataStore.getGenericRelations()) {

				if (!relation.getSubject().isEvent() && !relation.getObject().isEvent())
					continue;

				if (relation.getSubject().getId() == null) {
					String id = this.idGenerator.getID(relation.getSubject());
					if (id == null)
						continue;
					relation.getSubject().setId(id);
				}
				if (relation.getObject().getId() == null) {
					String id = this.idGenerator.getID(relation.getObject());
					if (id == null)
						continue;
					relation.getObject().setId(id);
				}

				String relationId = "eventkg_relation_" + String.valueOf(this.relationNo);
				this.relationNo += 1;

				writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
						"Relation", false, relation.getDataSet(), fileType);

				writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "subject", this.basePrefix, relation.getSubject().getId(),
						false, relation.getDataSet(), fileType);

				writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "object", this.basePrefix, relation.getObject().getId(),
						false, relation.getDataSet(), fileType);

				if (relation.getProperties() == null) {
					writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "roleType", relation.getPrefix(),
							relation.getProperty(), false, relation.getDataSet(), fileType);
				} else {
					for (SubProperty property : relation.getProperties()) {
						writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
								prefixList.getPrefix(PrefixEnum.SEM), "roleType", property.getPrefix(),
								property.getProperty(), false, property.getDataSet(), fileType);
					}
				}

				if (relation.getStartTime() != null) {
					writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasBeginTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getStartTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
							relation.getStartTime().getGranularity(), false, relation.getDataSet(), true, fileType);
				}
				if (relation.getEndTime() != null) {
					writeTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasEndTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getEndTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, this.relationNo, this.basePrefix, relationId,
							relation.getEndTime().getGranularity(), false, relation.getDataSet(), false, fileType);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

		this.relationNo += 1;

	}

	private void writeLiteralsRelations() {

		FileType fileType = FileType.NQ;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {

			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENT_LITERALS_RELATIONS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENT_LITERALS_RELATIONS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			// prefixes.add(Prefix.EVENT_KG_SCHEMA);
			// prefixes.add(Prefix.RDF);

			// add all prefixes, because they could all be in the
			// eventKGRelations
			for (Prefix prefix : prefixList.getAllPrefixes())
				prefixes.add(prefix);

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int withoutProperties = 0;
			int withoutPrefix = 0;
			int withoutPropertiesAndWithoutPrefix = 0;
			for (LiteralRelation relation : dataStore.getLiteralRelations()) {
				if (relation.getProperties() == null)
					withoutProperties += 1;
				if (relation.getPrefix() == null) {
					withoutPrefix += 1;
					if (relation.getProperties() == null)
						withoutPropertiesAndWithoutPrefix += 1;
				}
			}
			System.out.println("LiteralRelations: " + dataStore.getLiteralRelations().size());
			System.out.println(" without properties: " + withoutProperties);
			System.out.println(" without prefix: " + withoutPrefix);
			System.out.println(" without properties and without prefix: " + withoutPropertiesAndWithoutPrefix);

			int lineNo = 0;
			for (LiteralRelation relation : dataStore.getLiteralRelations()) {

				if (relation.getDataType() == null) {
					System.out.println("Missing data type. Ignore relation: " + relation.getSubject().getId() + "\t"
							+ relation.getProperty() + "\t" + relation.getObject());
					continue;
				}

				if (relation.getSubject().getId() == null) {
					String id = this.idGenerator.getEventID((Event) relation.getSubject());
					if (id == null)
						continue;
					relation.getSubject().setId(id);
				}

				String object = relation.getObject();

				lineNo += 1;
				String relationId = "eventkg_relation_" + String.valueOf(this.relationNo);
				this.relationNo += 1;

				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
						"Relation", false, relation.getDataSet(), fileType);
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "subject", this.basePrefix, relation.getSubject().getId(),
						false, relation.getDataSet(), fileType);

				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "object", null,
						createLiteral(object, relation.getDataType(), relation.getLanguageCode()), false,
						relation.getDataSet(), fileType);

				if (relation.getProperties() == null) {
					if (relation.getPrefix() == null) {
						System.out.println("Missing prefix: " + relation.getSubject().getId() + "\t"
								+ relation.getProperty() + "\t" + relation.getObject());
					}
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "roleType", relation.getPrefix(),
							relation.getProperty(), false, relation.getDataSet(), fileType);
				} else {
					for (SubProperty property : relation.getProperties()) {
						writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
								prefixList.getPrefix(PrefixEnum.SEM), "roleType", property.getPrefix(),
								property.getProperty(), false, property.getDataSet(), fileType);
					}
				}

				if (relation.getStartTime() != null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasBeginTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getStartTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							relation.getStartTime().getGranularity(), false, relation.getDataSet(), true, fileType);
				}
				if (relation.getEndTime() != null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasEndTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getEndTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							relation.getEndTime().getGranularity(), false, relation.getDataSet(), false, fileType);
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeEntityRelations() {

		FileType fileType = FileType.NQ;

		PrintWriter writerEntityTemporalRelations = null;
		PrintWriter writerEntityTemporalRelationsPreview = null;
		PrintWriter writerEntityRelations = null;
		PrintWriter writerEntityRelationsPreview = null;
		try {
			writerEntityTemporalRelations = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_TEMPORAL_RELATIONS);
			writerEntityTemporalRelationsPreview = FileLoader
					.getWriter(FileName.ALL_TTL_ENTITIES_TEMPORAL_RELATIONS_PREVIEW);
			writerEntityRelations = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_OTHER_RELATIONS);
			writerEntityRelationsPreview = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_OTHER_RELATIONS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			// prefixes.add(Prefix.EVENT_KG_SCHEMA);
			// prefixes.add(Prefix.RDF);

			// add all prefixes, because they could all be in the
			// eventKGRelations
			for (Prefix prefix : prefixList.getAllPrefixes())
				prefixes.add(prefix);

			for (String line : createIntro(prefixes, this.prefixList, fileType)) {
				writerEntityTemporalRelations.write(line + Config.NL);
				writerEntityTemporalRelationsPreview.write(line + Config.NL);
				writerEntityRelations.write(line + Config.NL);
				writerEntityRelationsPreview.write(line + Config.NL);
			}

			int lineNoEntityTemporalRelations = 0;
			int lineNoEntityRelations = 0;

			for (GenericRelation relation : dataStore.getGenericRelations()) {

				// ignore event relations here
				if (relation.getSubject().isEvent() || relation.getObject().isEvent())
					continue;

				Entity object = relation.getObject();

				if (relation.getSubject().getId() == null) {
					String id = this.idGenerator.getEntityID(relation.getSubject());
					if (id == null)
						continue;
					relation.getSubject().setId(id);
				}
				if (relation.getObject().getId() == null) {
					String id = this.idGenerator.getEntityID(relation.getObject());
					if (id == null)
						continue;
					relation.getObject().setId(id);
				}

				PrintWriter writer;
				PrintWriter writerPreview;
				Integer lineNo;

				// if (relation.isEntityRelation()) {
				if (relation.getStartTime() == null && relation.getEndTime() == null) {
					writer = writerEntityRelations;
					writerPreview = writerEntityRelationsPreview;
					lineNoEntityRelations += 1;
					lineNo = lineNoEntityRelations;
				} else {
					writer = writerEntityTemporalRelations;
					writerPreview = writerEntityTemporalRelationsPreview;
					lineNoEntityTemporalRelations += 1;
					lineNo = lineNoEntityTemporalRelations;
				}

				lineNo += 1;
				String relationId = "eventkg_relation_" + String.valueOf(this.relationNo);
				this.relationNo += 1;

				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "type", prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
						"Relation", false, relation.getDataSet(), fileType);
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "subject", this.basePrefix, relation.getSubject().getId(),
						false, relation.getDataSet(), fileType);
				writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
						prefixList.getPrefix(PrefixEnum.RDF), "object", this.basePrefix, object.getId(), false,
						relation.getDataSet(), fileType);

				if (relation.getProperties() == null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "roleType", relation.getPrefix(),
							relation.getProperty(), false, relation.getDataSet(), fileType);
				} else {
					for (SubProperty property : relation.getProperties()) {
						writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
								prefixList.getPrefix(PrefixEnum.SEM), "roleType", property.getPrefix(),
								property.getProperty(), false, property.getDataSet(), fileType);
					}
				}

				if (relation.getStartTime() != null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasBeginTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getStartTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							relation.getStartTime().getGranularity(), false, relation.getDataSet(), true, fileType);
				}
				if (relation.getEndTime() != null) {
					writeTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							prefixList.getPrefix(PrefixEnum.SEM), "hasEndTimeStamp", null,
							OUTPUT_DATE_FORMAT.format(relation.getEndTime().getDate()), false, relation.getDataSet(),
							fileType);
					writeDateGranularityTriple(writer, writerPreview, lineNo, this.basePrefix, relationId,
							relation.getEndTime().getGranularity(), false, relation.getDataSet(), false, fileType);
				}

				// if (relation.getPropertyLabels() != null) {
				// for (Language language :
				// relation.getPropertyLabels().keySet())
				// writeTriple(writer, writerPreview, lineNo, relationId,
				// Prefix.RDF.getAbbr() + "label",
				// relation.getPropertyLabels().get(language), true,
				// relation.getDataSet(), language);
				// }

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writerEntityTemporalRelations.close();
			writerEntityTemporalRelationsPreview.close();
			writerEntityRelations.close();
			writerEntityRelationsPreview.close();
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
			System.out.println(prefix);
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

		if (!ALLOW_DIRECTIVES && fileType == FileType.NQ)
			lines.clear();

		return lines;
	}

	// private void writeTriple(PrintWriter writer, String subject, String
	// property, String object, boolean quoteObject,
	// Language language, DataSet dataSet) {
	//
	// if (quoteObject) {
	// object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"@" +
	// language.getLanguageLowerCase();
	// }
	//
	// if (dataSet == null)
	// writer.write(subject + Config.SEP + property + Config.SEP + object +
	// Config.SEP + "." + Config.NL);
	// else
	// writer.write(subject + Config.SEP + property + Config.SEP + object +
	// Config.SEP + dataSet.getId()
	// + Config.SEP + "." + Config.NL);
	//
	// }

	private String createLiteral(String value, LiteralDataType dataType) {
		return createLiteral(value, dataType, null);
	}

	private String createLiteral(String value, LiteralDataType dataType, String languageCode) {

		String literal = value;

		// escape un-escaped quotation marks
		if (literal.contains("\"")) {
			literal = literal.replaceAll("(?<!\\\\)\"", "\\\\\"");
		}

		literal = "\"" + literal + "\"";

		if (languageCode != null)
			literal += "@" + languageCode;

		if (!dataType.getDataTypeRdf().isEmpty()) {
			if (!ALLOW_DIRECTIVES)
				literal += "^^<" + dataType.getDataTypeRdf().replace("xsd:", PrefixEnum.XSD.getUrlPrefix()) + ">";
			else
				literal += "^^" + dataType.getDataTypeRdf();
		}

		return literal;
	}

	public void writeDateGranularityTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo,
			Prefix subjectPrefix, String subject, DateGranularity granularity, boolean quoteObject, DataSet dataSet,
			boolean start, FileType fileType) {

		if (granularity == null)
			return;

		String property = null;
		String object = null;

		if (ALLOW_DIRECTIVES) {
			property = "eventKG-s:";
			if (start)
				property += "start";
			else
				property += "end";
			property += "UnitType";

			object = "";
			switch (granularity) {
			case DAY:
				object = "time:unitDay";
				break;
			case MONTH:
				object = "time:unitMonth";
				break;
			case YEAR:
				object = "time:unitYear";
				break;
			case DECADE:
				object = "eventKG-s:unitDecade";
				break;
			case CENTURY:
				object = "eventKG-s:unitCentury";
				break;
			default:
				break;
			}
		} else {
			property = "<" + prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getUrlPrefix();
			if (start)
				property += "start";
			else
				property += "end";
			property += "UnitType>";

			object = "";
			switch (granularity) {
			case DAY:
				object = "<" + prefixList.getPrefix(PrefixEnum.TIME).getUrlPrefix() + "unitDay>";
				break;
			case MONTH:
				object = "<" + prefixList.getPrefix(PrefixEnum.TIME).getUrlPrefix() + "unitMonth>";
				break;
			case YEAR:
				object = "<" + prefixList.getPrefix(PrefixEnum.TIME).getUrlPrefix() + "unitYear>";
				break;
			case DECADE:
				object = "<" + prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getUrlPrefix() + "unitDecade>";
				break;
			case CENTURY:
				object = "<" + prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getUrlPrefix() + "unitCentury>";
				break;
			default:
				break;
			}
		}

		String line = null;
		String subjectWithPrefix = createResourceWithPrefix(subjectPrefix, subject, fileType);

		if (dataSet == null) {
			line = subjectWithPrefix + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL;
		} else {
			String graph = createResourceWithPrefix(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
					fileType);
			line = subjectWithPrefix + Config.SEP + property + Config.SEP + object + Config.SEP + graph + Config.SEP
					+ "." + Config.NL;
		}

		writer.write(line);
		if (writerPreview != null && lineNo <= NUMBER_OF_LINES_IN_PREVIEW)
			writerPreview.write(line);
	}

	// private void writeTriple(PrintWriter writer, String subject, String
	// property, String object, boolean quoteObject,
	// Source source) {
	// if (quoteObject)
	// object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"";
	//
	// writer.write(subject + Config.SEP + property + Config.SEP + object +
	// Config.SEP + "." + Config.NL);
	// }

	public void writeTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo, Prefix subjectPrefix,
			String subject, Prefix propertyPrefix, String property, Prefix objectPrefix, String object,
			boolean quoteObject, DataSet dataSet, FileType fileType) {

		if (object == null)
			return;

		if (quoteObject) {
			object = object.replace("\\", "\\\\");
			object = object.replaceAll("\"", "\\\\\"");
			object = "\"" + object + "\"";
		}

		String line = null;

		String subjectWithPrefix = createResourceWithPrefix(subjectPrefix, subject, fileType);
		String propertyWithPrefix = createResourceWithPrefix(propertyPrefix, property, fileType);
		String objectWithPrefix = object;
		if (!quoteObject)
			objectWithPrefix = createResourceWithPrefix(objectPrefix, object, fileType);

		if (dataSet == null) {
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP
					+ "." + Config.NL;
		} else {
			String graph = createResourceWithPrefix(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
					fileType);
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP
					+ graph + Config.SEP + "." + Config.NL;
		}

		writer.write(line);
		if (writerPreview != null && lineNo <= NUMBER_OF_LINES_IN_PREVIEW)
			writerPreview.write(line);
	}

	public void writeTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo, Prefix subjectPrefix,
			String subject, Prefix propertyPrefix, String property, Prefix objectPrefix, String object,
			boolean quoteObject, DataSet dataSet, Language language, FileType fileType) {

		if (object == null)
			return;

		if (quoteObject) {
			object = object.replace("\\", "\\\\");
			object = object.replaceAll("\"", "\\\\\"");
			object = "\"" + object + "\"";
			if (language != null) {
				object += "@" + language.getLanguageLowerCase();
			}
		}

		// if (quoteObject && language != null) {
		// object = object.replace("\\", "\\\\");
		// object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"@" +
		// language.getLanguageLowerCase();
		// } else if (quoteObject) {
		// object = object.replace("\\", "\\\\");
		// object = "\"" + object.replaceAll("\"", "\\\\\"");
		// }

		String line = null;

		String subjectWithPrefix = createResourceWithPrefix(subjectPrefix, subject, fileType);
		String propertyWithPrefix = createResourceWithPrefix(propertyPrefix, property, fileType);
		String objectWithPrefix = object;
		if (!quoteObject)
			createResourceWithPrefix(objectPrefix, object, fileType);

		if (dataSet == null) {
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP
					+ "." + Config.NL;
		} else {
			String graph = createResourceWithPrefix(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId(),
					fileType);
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP
					+ graph + Config.SEP + "." + Config.NL;
		}

		writer.write(line);
		if (writerPreview != null && lineNo <= NUMBER_OF_LINES_IN_PREVIEW)
			writerPreview.write(line);

	}

	private String createResourceWithPrefix(Prefix prefix, String value, FileType fileType) {

		if (prefix == null)
			return value;

		String resourceWithPrefix = null;
		if (ALLOW_DIRECTIVES || fileType == FileType.TTL) {
			if (prefix == this.basePrefix)
				resourceWithPrefix = "<" + value + ">";
			else
				resourceWithPrefix = prefix.getAbbr() + value;
		} else {
			resourceWithPrefix = prefix.getUrlPrefix() + value;
			resourceWithPrefix = "<" + resourceWithPrefix + ">";
		}

		return resourceWithPrefix;
	}

	public Prefix getBasePrefix() {
		return basePrefix;
	}

	public void writeFirstSentence(Event event, Language language, String firstSentence, int lineNo, DataSet dataSet,
			PrintWriter writer, PrintWriter writerPreview) {

		String eventId = this.idGenerator.getEventID(event);
		if (eventId == null) {
			System.out.println("Missing event ID for " + event.getWikidataId() + ".");
			return;
		}

		writeTriple(writer, writerPreview, lineNo, this.basePrefix, eventId, prefixList.getPrefix(PrefixEnum.DCTERMS),
				"description", null, firstSentence, true, dataSet, language, FileType.NQ);
	}

	public void writeSubEvent(String parentEventEventKGId, String subEventEventKGId, int lineNo, DataSet dataSet,
			PrintWriter writer, PrintWriter writerPreview) {
		writeTriple(writer, writerPreview, lineNo, this.basePrefix, parentEventEventKGId,
				prefixList.getPrefix(PrefixEnum.SEM), "hasSubEvent", this.basePrefix, subEventEventKGId, false, dataSet,
				FileType.NQ);
	}

}
