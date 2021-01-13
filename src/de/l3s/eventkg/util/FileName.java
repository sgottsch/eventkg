package de.l3s.eventkg.util;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public enum FileName {

	// YAGO

	/**
	 * All "<happenenedOnDate>" and "<startedOnDate>" relations from YAGO.
	 * Update: now also died etc. for entities.
	 */
	YAGO_EXISTENCE_TIMES("yago_existence_times.txt", Source.YAGO, FileType.RESULTS, false, false, false),

	/**
	 * All "<isLocatedIn>" and "<happenedIn>" relations (event as subject) from
	 * YAGO
	 */
	YAGO_LOCATIONS("yago_locations.tsv", Source.YAGO, FileType.RESULTS, false, false, false),

	YAGO_EVENT_FACTS("yago_event_facts.tsv", Source.YAGO, FileType.RESULTS, false, false, false),

	YAGO_EVENT_LITERALS_FACTS("yago_event_literals_facts.tsv", Source.YAGO, FileType.RESULTS, false, false, false),

	YAGO_ENTITY_FACTS("yago_entity_facts.tsv", Source.YAGO, FileType.RESULTS, false, false, false),

	YAGO_POSITIONS("yago_positions.tsv", Source.YAGO, FileType.RESULTS, false, true, false),

	/**
	 * All YAGO facts that are assigned temporal information by reification
	 */
	YAGO_TEMPORAL_FACTS("yago_temporal_facts.tsv", Source.YAGO, FileType.RESULTS, false, false, false),

	// Wikidata

	// /**
	// * All Wikidata relations with "start time" (P580)
	// */
	// WIKIDATA_START_TIME("starttime-data.tsv", Source.WIKIDATA,
	// FileType.RESULTS, false, false, false),
	//
	// /**
	// * All Wikidata relations with "end time" (P582)
	// */
	// WIKIDATA_END_TIME("endtime-data.tsv", Source.WIKIDATA, FileType.RESULTS,
	// false, false, false),
	//
	// /**
	// * All Wikidata relations with "point in time" (P585)
	// */
	// WIKIDATA_POINT_IN_TIME("pointintime-data.tsv", Source.WIKIDATA,
	// FileType.RESULTS, false, false, false),

	ID_TO_WIKIPEDIA_MAPPING("id-to-wikipedia.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),

	WIKIDATA_LABELS("labels.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_LABELS_PROPERTIES("property_labels.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_DESCRIPTIONS("descriptions.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_DESCRIPTIONS_PROPERTIES(
			"property_descriptions.tsv",
			Source.WIKIDATA,
			FileType.RESULTS,
			false,
			false,
			false),
	WIKIDATA_ALIASES("aliases.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_ALIASES_PROPERTIES("property_aliases.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_TEMPORAL_PROPERTIES("temporal_properties.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_PART_OF("part-of-data.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_PART_OF_SERIES("part-of-series.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_FOLLOWS("follows-data.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_FOLLOWED_BY("followedby-data.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_SUBCLASS_OF("subclass-data.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_INSTANCE_OF("instanceof-data.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_PROPERTY_NAMES("property_names.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_EVENTS("event_instances.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_EVENT_SERIES("event_series_instances.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_RECURRENT_EVENT_EDITIONS(
			"event_series_editions.tsv",
			Source.WIKIDATA,
			FileType.RESULTS,
			false,
			false,
			false),
	WIKIDATA_NO_EVENTS("no_event_instances.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_LOCATIONS("wikidata_locations.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_SUB_LOCATIONS("wikidata_sub_locations.tsv", Source.WIKIDATA, FileType.RESULTS, false, true, false),
	WIKIDATA_POSITIONS("wikidata_positions.tsv", Source.WIKIDATA, FileType.RESULTS, false, true, false),
	WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME(
			"temporal_property_names.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_LOCATION_PROPERTY_NAMES(
			"property_names_locations.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_SUB_LOCATION_PROPERTY_NAMES(
			"properties_sublocations.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_EVENT_BLACKLIST_CLASSES(
			"event_blacklist_classes.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_POSITIVE_EVENT_CLASSES(
			"event_positive_classes.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_IGNORED_EVENT_CLASSES(
			"event_ignored_event_classes.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_EXTERNAL_IDS_PROPERTY_NAMES(
			"external_ids_property_names.json",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_WIKIPEDIA_INTERNAL_ITEMS(
			"wikipedia_internal_items.json",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_FORBIDDEN_CLASSES(
			"wikidata_forbidden_classes.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES(
			"forbidden_property_names.tsv",
			Source.WIKIDATA,
			FileType.META,
			false,
			false,
			false),
	WIKIDATA_TEMPORAL_FACTS("wikidata_temporal_facts.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),

	WIKIDATA_EVENT_RELATIONS("wikidata_event_relations.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_EVENT_LITERALS_RELATIONS(
			"wikidata_event_literals_relations.tsv",
			Source.WIKIDATA,
			FileType.RESULTS,
			false,
			false,
			false),
	WIKIDATA_ENTITY_RELATIONS("wikidata_entity_relations.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),
	WIKIDATA_PROPERTY_EQUALITIES(
			"wikidata_property_equalities.json",
			Source.WIKIDATA,
			FileType.RESULTS,
			false,
			false,
			false),

	WIKIDATA_VALID_IDS("wikidata_valid_ids.tsv", Source.WIKIDATA, FileType.RESULTS, false, false, false),

	// Wikipedia Current Events
	WCE_EVENTS("wce_events.tsv", Source.WCE, FileType.RESULTS, false, false, false),

	/**
	 * Entities that are mentioned in the same textual event as an event page
	 */
	WCE_EVENT_RELATIONS("wce_event_relations.tsv", Source.WCE, FileType.RESULTS, false, false, false),

	TEXT_EVENT_LINKS("text_event_links.tsv", Source.WIKIPEDIA, FileType.RESULTS, false, false, false),

	// DBpedia
	DBPEDIA_DBO_EVENTS_FILE_NAME("dbpedia_events.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_DBO_NO_EVENTS_FILE_NAME("dbpedia_no_events.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_DBO_EVENT_PARTS("dbpedia_event_parts.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_DBO_PREVIOUS_EVENTS("dbpedia_previous_events.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_DBO_NEXT_EVENTS("dbpedia_next_events.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_ALL_LOCATIONS("dbpedia_all_locations.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),
	DBPEDIA_NO_LOCATIONS("dbpedia_no_locations.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),

	/**
	 * Entities with locations, using "dbo:place".
	 */
	DBPEDIA_EVENT_LOCATIONS("dbpedia_event_locations.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),

	/**
	 * Entities with locations, using "dbo:place".
	 */
	DBPEDIA_TIMES("dbpedia_times.tsv", Source.DBPEDIA, FileType.RESULTS, false, true, false),

	/**
	 * Entities with positions (points).
	 */
	DBPEDIA_POSITIONS("dbpedia_positions.tsv", Source.DBPEDIA, FileType.RESULTS, false, true, false),

	/**
	 * Triples where the subject or the object is an event page.
	 */
	DBPEDIA_EVENT_RELATIONS("dbpedia_event_relations.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),

	/**
	 * Triples where the subject is an event page and the object is a literal.
	 */
	DBPEDIA_EVENT_LITERAL_RELATIONS(
			"dbpedia_event_literal_relations.tsv",
			Source.DBPEDIA,
			FileType.RESULTS,
			false,
			false,
			false),

	/**
	 * Triples where the subject and the object is an entity with existence time
	 * (but no event invovled).
	 */
	DBPEDIA_ENTITY_RELATIONS("dbpedia_entity_relations.tsv", Source.DBPEDIA, FileType.RESULTS, false, false, false),

	// Integrated results from multiple sources

	ALL_TTL_SCHEMA_INPUT("schema.ttl", Source.EVENT_KG, FileType.META, false, false, false),
	ALL_TTL_VOID_INPUT("void.ttl", Source.EVENT_KG, FileType.META, false, false, false),

	/**
	 * Wikipedia pages (or Wikidata items) representing events
	 */
	ALL_EVENT_PAGES("all_event_pages.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_ENTITIES_WITH_EXISTENCE_TIMES(
			"all_entities_with_existence_times.tsv",
			Source.ALL,
			FileType.RESULTS,
			false,
			false,
			false),
	// ALL_START_TIMES("all_start_times.tsv", Source.ALL, FileType.RESULTS,
	// false, false, false),
	// ALL_END_TIMES("all_end_times.tsv", Source.ALL, FileType.RESULTS, false,
	// false, false),

	ALL_TTL_EVENTS("events.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITIES("entities.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TEXT_EVENTS("text_events.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENTS_FIRST_SENTENCES("events_first_sentences.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENTS_FIRST_SENTENCES_PREVIEW(
			"events_first_sentences_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_EVENT_DESCRIPTIONS_FROM_TEXTUAL_EVENTS(
			"events_descriptions_from_text_events.nq",
			FileType.OUTPUT,
			false,
			false,
			false),
	ALL_TTL_EVENT_DESCRIPTIONS_FROM_TEXTUAL_EVENTS_PREVIEW(
			"events_descriptions_from_text_events_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_EVENTS_BASE_RELATIONS("relations_event_base.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITY_BASE_RELATIONS("relations_entity_base.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENTS_LINK_RELATIONS("event_links.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITY_LINK_RELATIONS("entity_links.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENTS_MENTIONS("event_mentions.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITY_MENTIONS("entity_mentions.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TEXT_EVENTS_LINKS("text_event_links.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENT_LITERALS_RELATIONS("relations_events_literals.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_EVENTS_OTHER_RELATIONS("relations_events_other.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITIES_TEMPORAL_RELATIONS("relations_entities_temporal.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_ENTITIES_OTHER_RELATIONS("relations_entities_other.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_PROPERTY_LABELS("property_labels.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_DATASETS("graphs.ttl", FileType.OUTPUT, false, false, false),
	ALL_TTL_SCHEMA("schema.ttl", FileType.OUTPUT, false, false, false),
	ALL_TTL_VOID("void.ttl", FileType.OUTPUT, false, false, false),
	ALL_NQ_TYPES("types.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TYPES_DBPEDIA("types_dbpedia.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TYPE_LABELS_DBPEDIA("type_labels_dbpedia.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TYPES_WIKIDATA("types_wikidata.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_TYPE_LABELS_WIKIDATA("type_labels_wikidata.nq", FileType.OUTPUT, false, false, false),
	ALL_TTL_DBPEDIA_ONTOLOGY("types_ontology_dbpedia.nq", FileType.OUTPUT, false, false, false),

	ALL_TTL_EVENTS_PREVIEW("events_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_ENTITIES_PREVIEW("entities_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TEXT_EVENTS_PREVIEW("text_events.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_EVENTS_BASE_RELATIONS_PREVIEW(
			"relations_event_base_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_ENTITY_BASE_RELATIONS_PREVIEW(
			"relations_entity_base_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_EVENTS_LINK_RELATIONS_PREVIEW("event_links_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_ENTITY_MENTIONS_PREVIEW("entity_mentions_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_EVENT_MENTIONS_PREVIEW("event_mentions_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TEXT_EVENTS_LINKS_PREVIEW("text_event_links_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_ADDITIONAL_SUB_EVENTS_PREVIEW(
			"relations_additional_sub_events_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_ENTITY_LINK_RELATIONS_PREVIEW("entity_links_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_PROPERTY_LABELS_PREVIEW("property_labels_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_EVENT_LITERALS_RELATIONS_PREVIEW(
			"relations_events_literals_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_EVENTS_OTHER_RELATIONS_PREVIEW(
			"relations_events_other_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_ENTITIES_TEMPORAL_RELATIONS_PREVIEW(
			"relations_entities_temporal_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_ENTITIES_OTHER_RELATIONS_PREVIEW(
			"relations_entities_other_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),
	ALL_TTL_DBPEDIA_ONTOLOGY_PREVIEW("types_ontology_dbpedia_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_NQ_TYPES_PREVIEW("types_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TYPES_DBPEDIA_PREVIEW("types_dbpedia_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TYPE_LABELS_DBPEDIA_PREVIEW("type_labels_dbpedia_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TYPES_WIKIDATA_PREVIEW("types_wikidata_preview.nq", FileType.OUTPUT_PREVIEW, false, false, false),
	ALL_TTL_TYPE_LABELS_WIKIDATA_PREVIEW(
			"type_labels_wikidata_preview.nq",
			FileType.OUTPUT_PREVIEW,
			false,
			false,
			false),

	ALL_TTL_ENTITIES_PREVIOUS_VERSION("entities.nq", FileType.PREVIOUS_VERSION, false, false, false),
	ALL_TTL_EVENTS_PREVIOUS_VERSION("events.nq", FileType.PREVIOUS_VERSION, false, false, false),
	ALL_TTL_TEXT_EVENTS_PREVIOUS_VERSION("text_events.nq", FileType.PREVIOUS_VERSION, false, false, false),
	// ALL_TTL_EVENTS_OTHER_RELATIONS_PREVIOUS_VERSION(
	// "relations_other.nq",
	// FileType.PREVIOUS_VERSION,
	// false,
	// false,
	// false),
	// ALL_TTL_ENTITIES_OTHER_RELATIONS_PREVIOUS_VERSION(
	// "relations_entities_other.nq",
	// FileType.PREVIOUS_VERSION,
	// false,
	// false,
	// false),
	// ALL_TTL_ENTITIES_TEMPORAL_RELATIONS_PREVIOUS_VERSION(
	// "relations_entities_temporal.nq",
	// FileType.PREVIOUS_VERSION,
	// false,
	// false,
	// false),
	// ALL_TTL_EVENTS_LINK_RELATIOINS_PREVIOUS_VERSION(
	// "relations_links.nq",
	// FileType.PREVIOUS_VERSION,
	// false,
	// false,
	// false),

	/**
	 * Event pages with times
	 */
	ALL_EVENT_TIMES("all_event_times.tsv", Source.ALL, FileType.RESULTS, false, false, false),

	/**
	 * Event pages with part of relations
	 */
	ALL_PART_OF_RELATIONS("all_event_part_ofs.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_PREVIOUS_EVENTS("all_previous_events.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_NEXT_EVENTS("all_next_events.tsv", Source.ALL, FileType.RESULTS, false, false, false),

	/**
	 * Event pages with the events' locations
	 */
	ALL_LOCATIONS("all_event_locations.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_SUB_LOCATIONS("all_sub_locations.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_POSITIONS("all_positions.tsv", Source.ALL, FileType.RESULTS, false, false, false),

	ALL_TEMPORAL_RELATIONS("all_temporal_relations.tsv", Source.ALL, FileType.RESULTS, false, false, false),

	ALL_LINK_COUNTS("link_counts.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_LINKED_BY_COUNTS("linked_by_counts.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_LINK_SETS("link_sets.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_FIRST_SENTENCES("first_sentences.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_TEXTUAL_EVENTS("textual_events.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_TEXTUAL_EVENTS_EVENTS("textual_events_events.tsv", Source.ALL, FileType.RESULTS, false, false, false),
	ALL_TEXTUAL_EVENTS_ENTITIES("textual_events_entities.tsv", Source.ALL, FileType.RESULTS, false, false, false),

	// Raw data

	SEM_ONTOLOGY("sem_ontology.rdf", Source.ALL, FileType.RAW_DATA, false, false, false),

	/**
	 * Gzipped Wikidata dump
	 */
	WIKIDATA_DUMP("dump.json.gz", Source.WIKIDATA, FileType.RAW_DATA, false, false, false),

	DBPEDIA_ONTOLOGY("dbpedia_ontology.nt", Source.DBPEDIA, FileType.RAW_DATA, false, false, false),
	DBPEDIA_TYPES("instance_types_$lang$.ttl", Source.DBPEDIA, FileType.RAW_DATA, false, false, false),
	// DBPEDIA_TYPES_TRANSITIVE(
	// "instance_types_transitive_$lang$.ttl",
	// Source.DBPEDIA,
	// FileType.RAW_DATA,
	// false,
	// false,
	// false),
	DBPEDIA_MAPPINGS("mappingbased_objects_$lang$.ttl", Source.DBPEDIA, FileType.RAW_DATA, false, false, false),
	DBPEDIA_MAPPINGS_LITERALS(
			"mappingbased_literals_$lang$.ttl",
			Source.DBPEDIA,
			FileType.RAW_DATA,
			false,
			false,
			false),
	// DBPEDIA_GEONAMES_LINKS("geonames_links_$lang$.ttl", Source.DBPEDIA,
	// FileType.RAW_DATA, false, false, false),
	DBPEDIA_GEO_COORDINATES("geo_coordinates_$lang$.ttl", Source.DBPEDIA, FileType.RAW_DATA, false, false, false),

	DBPEDIA_PART_OF_PROPERTIES("part_of_properties.tsv", Source.DBPEDIA, FileType.META, false, false, false),

	// YAGO
	YAGO_TAXONOMY("yagoTaxonomy.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),
	YAGO_WIKIDATA_INSTANCES("yagoWikidataInstances.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),

	/**
	 * YAGO facts that have a temporal expression as object.
	 */
	YAGO_DATE_FACTS("yagoDateFacts.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),
	YAGO_LITERAL_FACTS("yagoLiteralFacts.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),
	YAGO_FACTS("yagoFacts.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),

	/**
	 * YAGO facts describing other facts (by reification), e.g. validity times
	 */
	YAGO_META_FACTS("yagoMetaFacts.ttl", Source.YAGO, FileType.RAW_DATA, false, false, false),
	YAGO_TIME_PROPERTIES("time_properties.tsv", Source.YAGO, FileType.META, false, false, false),

	YAGO_TO_DBPEDIA_RELATIONS(
			"yago_relations_yago_to_dbpedia.tsv",
			Source.YAGO,
			FileType.RAW_DATA,
			false,
			false,
			false),
	YAGO_FROM_DBPEDIA_RELATIONS(
			"yago_relations_dbpedia_to_yago.tsv",
			Source.YAGO,
			FileType.RAW_DATA,
			false,
			false,
			false),

	// Wikipedia Current Events
	WCE_EVENTS_WIKIPEDIA_FOLDER("events_", Source.WCE, FileType.RAW_DATA, true, false, false),
	WCE_EVENTS_FOLDER("events_", Source.WCE, FileType.RESULTS, true, false, false),

	// Wikipedia
	WIKIPEDIA_REDIRECTS("redirects.sql.gz", Source.WIKIPEDIA, FileType.RAW_DATA, false, false, true),
	WIKIPEDIA_PAGE_INFOS("page.sql.gz", Source.WIKIPEDIA, FileType.RAW_DATA, false, false, true),
	WIKIPEDIA_CATEGORYLINKS("categorylinks.sql.gz", Source.WIKIPEDIA, FileType.RAW_DATA, false, false, true),
	WIKIPEDIA_META_WORDS("words.txt", Source.WIKIPEDIA, FileType.META, false, false, false),
	WIKIPEDIA_META_EVENT_DATE_EXPRESSIONS(
			"event_date_expressions.txt",
			Source.WIKIPEDIA,
			FileType.META,
			false,
			false,
			false),

	WIKIPEDIA_DUMP_FILE_LIST("dump_file_list.txt", Source.WIKIPEDIA, FileType.RAW_DATA, false, false, false),

	WIKIPEDIA_DUMPS("dumps", Source.WIKIPEDIA, FileType.RAW_DATA, true, false, false),

	WIKIPEDIA_LINK_SETS("link_sets-", Source.WIKIPEDIA, FileType.RESULTS, true, false, false),
	WIKIPEDIA_LINK_COUNTS("link_counts-", Source.WIKIPEDIA, FileType.RESULTS, true, false, false),
	WIKIPEDIA_TEXTUAL_EVENTS("events-", Source.WIKIPEDIA, FileType.RESULTS, true, false, false),

	WIKIPEDIA_FIRST_SENTENCES("first_sentences-", Source.WIKIPEDIA, FileType.RESULTS, true, false, false),
	WIKIPEDIA_EVENTS("event_instances.tsv", Source.WIKIPEDIA, FileType.RESULTS, false, false, false),
	TIMES_INTEGRATED("cimes_integrated.csv", Source.ALL, FileType.RESULTS, false, false, false),
	CONNECTED_ENTITIES("connected_entities.csv", Source.ALL, FileType.RESULTS, false, false, false);

	private Source source;

	// in case of a folder, fileName are the files' prefixes
	private String fileName;
	private FileType fileType;
	private boolean isFolder;
	private boolean hasColumnNamesInFirstLine;

	private boolean isGZipped;

	// FileName(String fileName, Source source, boolean isRawData, boolean
	// isFolder, boolean hasColumnNamesInFirstLine) {
	// this.source = source;
	// this.fileName = fileName;
	// this.isRawData = isRawData;
	// this.isFolder = false;
	// }

	FileName(String fileName, FileType fileType, boolean isFolder, boolean hasColumnNamesInFirstLine,
			boolean isGZipped) {
		this.fileName = fileName;
		this.fileType = fileType;
		this.isFolder = isFolder;
		this.isGZipped = isGZipped;
		this.hasColumnNamesInFirstLine = hasColumnNamesInFirstLine;
	}

	FileName(String fileName, Source source, FileType fileType, boolean isFolder, boolean hasColumnNamesInFirstLine,
			boolean isGZipped) {
		this.source = source;
		this.fileName = fileName;
		this.fileType = fileType;
		this.isFolder = isFolder;
		this.isGZipped = isGZipped;
		this.hasColumnNamesInFirstLine = hasColumnNamesInFirstLine;
	}

	public Source getSource() {
		return source;
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileName(Language language) {
		return fileName.replace("$lang$", language.getLanguageLowerCase());
	}

	public boolean isRawData() {
		return this.fileType == FileType.RAW_DATA;
	}

	public boolean isResultsData() {
		return this.fileType == FileType.RESULTS;
	}

	public boolean isMetaData() {
		return this.fileType == FileType.META;
	}

	public boolean isOutputData() {
		return this.fileType == FileType.OUTPUT;
	}

	public boolean isOutputLightData() {
		return this.fileType == FileType.OUTPUT_LIGHT;
	}

	public boolean isOutputPreviewData() {
		return this.fileType == FileType.OUTPUT_PREVIEW;
	}

	public boolean isOutputLightPreviewData() {
		return this.fileType == FileType.OUTPUT_LIGHT_PREVIEW;
	}

	public boolean isPreviousVersionData() {
		return this.fileType == FileType.PREVIOUS_VERSION;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public boolean hasColumnNamesInFirstLine() {
		return hasColumnNamesInFirstLine;
	}

	public boolean isGZipped() {
		return isGZipped;
	}

	private enum FileType {
		RAW_DATA,
		RESULTS,
		META,
		OUTPUT,
		OUTPUT_PREVIEW,
		OUTPUT_LIGHT,
		OUTPUT_LIGHT_PREVIEW,
		PREVIOUS_VERSION;
	}

}