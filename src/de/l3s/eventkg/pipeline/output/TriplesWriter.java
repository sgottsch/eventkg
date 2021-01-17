package de.l3s.eventkg.pipeline.output;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.EntityIdGenerator;
import de.l3s.eventkg.integration.collection.EventDependency;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.LiteralDataType;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.SubProperty;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.source.wikipedia.model.LinksToCountNew;

public class TriplesWriter {

	private RDFWriterStore writers;

	private PrefixList prefixList;

	private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat(
			"\"yyyy-MM-dd\"'^^<" + PrefixEnum.XSD.getUrlPrefix() + "date>'");
	private Prefix basePrefix;

	private EntityIdGenerator idGenerator;

	private int literalRelationNo = 0;
	private int genericRelationNo = 0;
	private int linksToRelationNo = 0;
	private int coMentionRelationNo = 0;
	private int textLinkRelationNo = 0;

	private Set<RDFWriter> currentlyUsedWriters;
	private Set<RDFWriter> currentlyUsedWritersLight;

	public TriplesWriter(boolean useIDsFromPreviousVersion) {

		this.idGenerator = new EntityIdGenerator(useIDsFromPreviousVersion);

		this.writers = new RDFWriterStore();

		this.prefixList = PrefixList.getInstance();
		this.basePrefix = prefixList.getPrefix(PrefixEnum.EVENT_KG_RESOURCE);
	}

	/**
	 * The preview files can contain a predefined set of instances (i.e., a set
	 * of lines about one event). By calling startInstance and {@endInstance},
	 * you can encapsulate the calls for one instance.
	 */
	public void startInstance() {
		this.currentlyUsedWriters = new HashSet<RDFWriter>();
		this.currentlyUsedWritersLight = new HashSet<RDFWriter>();
	}

	public void endInstance() {
		for (RDFWriter writer : currentlyUsedWriters) {
			writer.increasePreviewLineCount();
		}
		for (RDFWriter writer : currentlyUsedWritersLight) {
			writer.increasePreviewLineCountLight();
		}
		this.currentlyUsedWriters = new HashSet<RDFWriter>();
		this.currentlyUsedWritersLight = new HashSet<RDFWriter>();
	}

	public void writeTime(Entity entity, DateWithGranularity date, DataSet dataSet, boolean isStartTime,
			boolean printLight) {

		RDFWriter writer = null;
		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENT_BASE_RELATIONS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITY_BASE_RELATIONS, printLight);

		String property = "hasBeginTimeStamp";
		if (!isStartTime)
			property = "hasEndTimeStamp";

		String entityId = getId(entity);
		if (entityId == null) {
			System.out.println("Entity has no ID: " + entity.getWikidataId() + " / " + entity.getWikidataLabels());
			return;
		}

		LinePair line = createTriple(this.basePrefix, entityId, prefixList.getPrefix(PrefixEnum.SEM), property, null,
				OUTPUT_DATE_FORMAT.format(date.getDate()), false, null, dataSet);
		LinePair granularityLine = createDateGranularityTriple(this.basePrefix, entity.getId(), date.getGranularity(),
				dataSet, isStartTime);

		if (line != null) {
			writer.write(line, printLight);
			if (granularityLine != null) {
				writer.write(granularityLine, printLight);
			}
		}
	}

	public void writeEventLocation(Event event, Entity location, DataSet dataSet, boolean printLight) {

		RDFWriter writer = getWriter(RDFWriterName.EVENT_BASE_RELATIONS, printLight);

		String entityId = getId(event);
		if (entityId == null) {
			System.out.println("Event has no ID: " + event.getWikidataId() + " / " + event.getWikidataLabels());
			return;
		}
		String locationId = getId(location);
		if (locationId == null) {
			System.out
					.println("Location has no ID: " + location.getWikidataId() + " / " + location.getWikidataLabels());
			return;
		}

		LinePair line = createTriple(this.basePrefix, entityId, prefixList.getPrefix(PrefixEnum.SEM), "hasPlace",
				this.basePrefix, locationId, false, null, dataSet);

		if (line != null) {
			writer.write(line, printLight);
		}
	}

	public void writePosition(Entity entity, Position position, DataSet dataSet, boolean printLight) {

		RDFWriter writer = null;
		if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENT_BASE_RELATIONS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITY_BASE_RELATIONS, printLight);

		String entityId = getId(entity);
		if (entityId == null) {
			System.out.println("Entity has no ID: " + entity.getWikidataId() + " / " + entity.getWikidataLabels());
			return;
		}

		LinePair lineLatitude = createTriple(this.basePrefix, entityId, prefixList.getPrefix(PrefixEnum.SCHEMA_ORG),
				"latitude", null, createLiteral(String.valueOf(position.getLatitude()), LiteralDataType.DOUBLE), false,
				null, dataSet);
		LinePair lineLongitude = createTriple(this.basePrefix, entityId, prefixList.getPrefix(PrefixEnum.SCHEMA_ORG),
				"longitude", null, createLiteral(String.valueOf(position.getLongitude()), LiteralDataType.DOUBLE), false,
				null, dataSet);

		if (lineLatitude != null)
			writer.write(lineLatitude, printLight);
		if (lineLongitude != null)
			writer.write(lineLongitude, printLight);
	}

	private RDFWriter getWriter(RDFWriterName writerName, boolean printLight) {
		RDFWriter writer = writers.getWriter(writerName);

		if (this.currentlyUsedWriters != null) {
			this.currentlyUsedWriters.add(writer);

			if (printLight)
				this.currentlyUsedWritersLight.add(writer);
		}

		return writer;
	}

	public void writeBasicTypeTriple(Entity entity, Prefix prefix, String type, DataSet dataSet, boolean printLight) {

		RDFWriter writer = null;
		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeTypeTriple(writer, entity, prefix, type, dataSet, printLight);
	}

	public void writeDBPediaTypeTriple(Entity entity, Prefix prefix, String type, DataSet dataSet, boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.DBPEDIA_TYPES, printLight);

		writeTypeTriple(writer, entity, prefix, type, dataSet, printLight);
	}

	public void writeDBPediaTypeLabelTriple(String type, String label, DataSet dataSet, Language language,
			boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.DBPEDIA_TYPE_LABELS, printLight);

		writer.write(
				createTriple(prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), type,
						prefixList.getPrefix(PrefixEnum.RDFS), "label", null, label, true, language, dataSet),
				printLight);
	}

	public void writeWikidataTypeLabelTriple(String type, String label, DataSet dataSet, Language language,
			boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.WIKIDATA_TYPE_LABELS, printLight);

		writer.write(
				createTriple(prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY), type,
						prefixList.getPrefix(PrefixEnum.RDFS), "label", null, label, true, language, dataSet),
				printLight);
	}

	public void writeWikidataTypeTriple(Entity entity, Prefix prefix, String type, DataSet dataSet,
			boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.WIKIDATA_TYPES, printLight);

		writeTypeTriple(writer, entity, prefix, type, dataSet, printLight);
	}

	public void writeDBPediaOntologyTypeTriple(String type1, Prefix prefix, String type, DataSet dataSet,
			boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.DBPEDIA_ONTOLOGY, printLight);

		Prefix prefix1 = prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY);

		writeTypeTriple(writer, prefix1, type1, prefix, type, dataSet, printLight);
	}

	public void writeDBPediaOntologySubClassTriple(String type1, String type2, DataSet dataSet, boolean printLight) {
		Prefix prefix1 = prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY);
		Prefix prefix2 = prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY);

		RDFWriter writer = getWriter(RDFWriterName.DBPEDIA_ONTOLOGY, printLight);

		LinePair line = createTriple(prefix1, type1, prefixList.getPrefix(PrefixEnum.RDFS), "subClassOf", prefix2,
				type2, false, null, dataSet);

		writer.write(line, printLight);
	}

	public void writeTypeTriple(Entity entity, Prefix prefix, String type, DataSet dataSet, boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.TYPES, printLight);
		writeTypeTriple(writer, entity, prefix, type, dataSet, printLight);
	}

	private void writeTypeTriple(RDFWriter writer, Entity entity, Prefix prefix, String type, DataSet dataSet,
			boolean printLight) {
		writeTriple(writer, entity, prefixList.getPrefix(PrefixEnum.RDF), "type", prefix, type, dataSet, printLight);
	}

	private void writeTypeTriple(RDFWriter writer, Prefix subjectPrefix, String subject, Prefix prefix, String type,
			DataSet dataSet, boolean printLight) {
		writeTriple(writer, subjectPrefix, subject, prefixList.getPrefix(PrefixEnum.RDF), "type", prefix, type, dataSet,
				printLight);
	}

	public void writeBasicLabelTriple(Entity entity, String label, DataSet dataSet, Language language,
			boolean printLight) {
		RDFWriter writer = null;

		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeLabelTriple(writer, entity, label, dataSet, language, printLight);
	}

	public void writeBasicAliasTriple(Entity entity, String label, DataSet dataSet, Language language,
			boolean printLight) {

		RDFWriter writer = null;
		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeAliasTriple(writer, entity, label, dataSet, language, printLight);
	}

	public void writeBasicDescriptionTriple(Entity entity, String label, DataSet dataSet, Language language,
			boolean printLight) {

		RDFWriter writer = null;
		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeDescriptionTriple(writer, entity, label, dataSet, language, printLight);
	}

	public void writeTextEventSourceTriple(Entity entity, String source, DataSet dataSet, boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);

		writeURITriple(writer, entity, prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), "source", source, dataSet,
				printLight);
	}

	public void writeTextEventCategoryTriple(Event event, String category, Language language, DataSet dataSet,
			boolean printLight) {
		RDFWriter writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);

		writer.write(createTriple(this.basePrefix, event.getId(), prefixList.getPrefix(PrefixEnum.DCTERMS), "title",
				null, createLiteral(category, LiteralDataType.LANG_STRING, language.getLanguageLowerCase()), false,
				null, dataSet), true);
	}

	public void writeBasicExtractedTriple(Entity entity, String uri, DataSet dataSet, boolean printLight) {

		RDFWriter writer = null;
		if (entity.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeExtractedFromTriple(writer, entity, uri, dataSet, printLight);
	}

	private void writeLabelTriple(RDFWriter writer, Entity entity, String label, DataSet dataSet, Language language,
			boolean printLight) {
		writeLanguageStringTriple(writer, entity, prefixList.getPrefix(PrefixEnum.RDFS), "label", label, dataSet,
				language, printLight);
	}

	private void writeDescriptionTriple(RDFWriter writer, Entity entity, String description, DataSet dataSet,
			Language language, boolean printLight) {
		writeLanguageStringTriple(writer, entity, prefixList.getPrefix(PrefixEnum.DCTERMS), "description", description,
				dataSet, language, printLight);
	}

	private void writeAliasTriple(RDFWriter writer, Entity entity, String alias, DataSet dataSet, Language language,
			boolean printLight) {
		writeLanguageStringTriple(writer, entity, prefixList.getPrefix(PrefixEnum.DCTERMS), "alternative", alias,
				dataSet, language, printLight);
	}

	private void writeExtractedFromTriple(RDFWriter writer, Entity entity, String uri, DataSet dataSet,
			boolean printLight) {
		writeURITriple(writer, entity, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "extractedFrom", uri, dataSet,
				printLight);
	}

	public void writeBasicSameAsTriple(Entity entity, Prefix prefix, String type, DataSet dataSet, boolean printLight) {

		if (type.contains("\\")) {
			System.out.println("\\ not allowed: " + type);
			return;
		}

		RDFWriter writer = null;
		if (entity.isEvent())
			writer = getWriter(RDFWriterName.EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.ENTITIES, printLight);

		writeSameAsTriple(writer, entity, prefix, type, dataSet, printLight);
	}

	private void writeSameAsTriple(RDFWriter writer, Entity entity, Prefix prefix, String type, DataSet dataSet,
			boolean printLight) {
		writeTriple(writer, entity, prefixList.getPrefix(PrefixEnum.OWL), "sameAs", prefix, type, dataSet, printLight);
	}

	public void writeSubLocation(Entity location, Entity subLocation, boolean printLight) {

		String locationId = getId(location);
		if (locationId == null) {
			System.out
					.println("Location has no ID: " + location.getWikidataId() + " / " + location.getWikidataLabels());
			return;
		}

		String subLocationId = getId(subLocation);
		if (subLocationId == null) {
			System.out.println(
					"Sub location has no ID: " + subLocation.getWikidataId() + " / " + subLocation.getWikidataLabels());
			return;
		}
		RDFWriter writer = getWriter(RDFWriterName.ENTITY_BASE_RELATIONS, printLight);

		LinePair line = createTriple(this.basePrefix, subLocationId, prefixList.getPrefix(PrefixEnum.SCHEMA_ORG),
				"containedInPlace", this.basePrefix, locationId, false, null, null);

		if (line != null) {
			writer.write(line, printLight);
		}
	}

	public void writeEventDependency(Event event1, Event event2, EventDependency eventDepdendency, boolean printLight) {

		String event1Id = getId(event1);
		String event2Id = getId(event2);

		if (event1Id == null) {
			System.out.println("EventDepdencies: Event 1 has no ID: " + event1.getWikidataId() + " / "
					+ event1.getWikidataLabels());
			return;
		}
		if (event2Id == null) {
			System.out.println("EventDepdencies: Event 2 has no ID: " + event2.getWikidataId() + " / "
					+ event2.getWikidataLabels());
			return;
		}

		RDFWriter writer = null;
		if (event1.isTextEvent() || event2.isTextEvent())
			writer = getWriter(RDFWriterName.TEXT_EVENTS, printLight);
		else
			writer = getWriter(RDFWriterName.EVENT_BASE_RELATIONS, printLight);

		LinePair line = createTriple(this.basePrefix, event1Id, prefixList.getPrefix(eventDepdendency.getPrefix()),
				eventDepdendency.getProperty(), this.basePrefix, event2Id, false, null, null);

		if (line != null) {
			writer.write(line, printLight);
		}
	}

	public void writeEventFirstSentence(Event event, Language language, String description, DataSet dataSet) {

		RDFWriter writer = getWriter(RDFWriterName.EVENT_FIRST_SENTENCES, true);

		writeLanguageStringTriple(writer, event, prefixList.getPrefix(PrefixEnum.DCTERMS), "description", description,
				dataSet, language, true);
	}

	public void writeEventTextualEventDescription(Event event, Language language, String description, DataSet dataSet) {
		RDFWriter writer = getWriter(RDFWriterName.EVENT_DESCRIPTIONS_FROM_TEXTUAL_EVENTS, true);

		writeLanguageStringTriple(writer, event, prefixList.getPrefix(PrefixEnum.DCTERMS), "description", description,
				dataSet, language, true);
	}

	private String getId(Entity entity) {
		if (entity.getId() == null) {
			String eventId = this.idGenerator.getID(entity);
			entity.setId(eventId);
		}
		return entity.getId();
	}

	public void close() {
		writers.closeWriters();
	};

	public LinePair createDateGranularityTriple(Prefix subjectPrefix, String subject, DateGranularity granularity,
			DataSet dataSet, boolean start) {

		if (granularity == null)
			return null;

		String property = null;
		String object = null;

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

		String line = null;
		String lineLight = null;
		String subjectWithPrefix = createResourceWithPrefix(subjectPrefix, subject);

		if (dataSet == null) {
			line = subjectWithPrefix + Config.SEP + property + Config.SEP + object + Config.SEP + ".";
			lineLight = line;
		} else {
			String graph = createResourceWithPrefix(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId());
			line = subjectWithPrefix + Config.SEP + property + Config.SEP + object + Config.SEP;
			lineLight = line + ".";
			line = line + graph + Config.SEP + ".";
		}

		LinePair linePair = new LinePair(line, lineLight);

		return linePair;
	}

	private String createResourceWithPrefix(Prefix prefix, String value) {

		if (prefix == null)
			return value;

		String resourceWithPrefix = prefix.getUrlPrefix() + value;
		resourceWithPrefix = "<" + resourceWithPrefix + ">";

		return resourceWithPrefix;
	}

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
			literal += "^^<" + dataType.getDataTypeRdf().replace("xsd:", PrefixEnum.XSD.getUrlPrefix()) + ">";
		}

		return literal;
	}

	private void writeTriple(RDFWriter writer, Prefix subjectPrefix, String subject, Prefix propertyPrefix,
			String property, Prefix objectPrefix, String object, DataSet dataSet, boolean printLight) {

		LinePair line = createTriple(subjectPrefix, subject, propertyPrefix, property, objectPrefix, object, false,
				null, dataSet);

		writer.write(line, printLight);
	}

	private void writeTriple(RDFWriter writer, Entity entity, Prefix propertyPrefix, String property,
			Prefix objectPrefix, String object, DataSet dataSet, boolean printLight) {

		String entityId = getId(entity);
		if (entityId == null) {
			System.out.println("Entity has no ID: " + entity.getWikidataId() + " / " + entity.getWikidataLabels());
			return;
		}

		LinePair line = createTriple(this.basePrefix, entityId, propertyPrefix, property, objectPrefix, object, false,
				null, dataSet);

		writer.write(line, printLight);
	}

	private void writeLanguageStringTriple(RDFWriter writer, Entity entity, Prefix propertyPrefix, String property,
			String type, DataSet dataSet, Language language, boolean printLight) {

		String entityId = getId(entity);
		if (entityId == null) {
			System.out.println("Entity has no ID: " + entity.getWikidataId() + " / " + entity.getWikidataLabels());
			return;
		}

		LinePair line = createTriple(this.basePrefix, entityId, propertyPrefix, property, null, type, true, language,
				dataSet);

		writer.write(line, printLight);
	}

	private void writeURITriple(RDFWriter writer, Entity entity, Prefix propertyPrefix, String property, String uri,
			DataSet dataSet, boolean printLight) {

		String entityId = getId(entity);
		if (entityId == null) {
			System.out.println("Entity has no ID: " + entity.getWikidataId() + " / " + entity.getWikidataLabels());
			return;
		}

		if (uri.contains("\\")) {
			System.out.println("Can't store URI in a triple: " + uri);
			return;
		}

		LinePair line = createTriple(this.basePrefix, entityId, propertyPrefix, property, null, "<" + uri + ">", false,
				null, dataSet);

		writer.write(line, printLight);

		// writer.increasePreviewLineCount();
		// if (printLight)
		// writer.increasePreviewLineCountLight();
	}

	public LinePair createTriple(Prefix subjectPrefix, String subject, Prefix propertyPrefix, String property,
			Date date, DataSet dataSet) {
		return createTriple(subjectPrefix, subject, propertyPrefix, property, null, OUTPUT_DATE_FORMAT.format(date),
				false, null, dataSet);
	}

	public LinePair createTriple(Prefix subjectPrefix, String subject, Prefix propertyPrefix, String property,
			Prefix objectPrefix, String object, boolean quoteObject, Language language, DataSet dataSet) {

		if (object == null) {
			System.out.println("Object is null: " + subject + ", " + property + ", " + object);
			return null;
		}

		if (quoteObject) {
			object = object.replace("\\", "\\\\");
			object = object.replaceAll("\"", "\\\\\"");
			object = "\"" + object + "\"";
			if (language != null)
				object += "@" + language.getLanguageLowerCase();
		}

		String line = null;
		String lineLight = null;

		String subjectWithPrefix = createResourceWithPrefix(subjectPrefix, subject);
		String propertyWithPrefix = createResourceWithPrefix(propertyPrefix, property);
		String objectWithPrefix = object;
		if (!quoteObject)
			objectWithPrefix = createResourceWithPrefix(objectPrefix, object);

		if (dataSet == null) {
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP
					+ ".";
			lineLight = line;
		} else {
			String graph = createResourceWithPrefix(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId());
			line = subjectWithPrefix + Config.SEP + propertyWithPrefix + Config.SEP + objectWithPrefix + Config.SEP;
			lineLight = line + ".";
			line += graph + Config.SEP + ".";
		}

		LinePair linePair = new LinePair(line, lineLight);

		return linePair;
	}

	public RDFWriter getWriter(RDFWriterName name) {
		return writers.getWriter(name);
	}

	public void resetNumberOfInstances(RDFWriterName name) {
		writers.getWriter(name).resetNumberOfLines();
	}

	public void writeDataSetDescription(String name, Language language, String description, String homepage,
			Date date) {

		RDFWriter writer = getWriter(RDFWriterName.VOID, true);

		Prefix graphPrefix = prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH);

		writer.write(createTriple(graphPrefix, "event_kg", prefixList.getPrefix(PrefixEnum.DCTERMS), "source",
				prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH), name, false, null, null), true);

		writer.write(createTriple(graphPrefix, name, prefixList.getPrefix(PrefixEnum.RDF), "type",
				prefixList.getPrefix(PrefixEnum.VOID), "Dataset", false, null, null), true);

		if (language != null) {
			String languageCode = language.getLanguageLowerCase();
			writer.write(createTriple(graphPrefix, name, prefixList.getPrefix(PrefixEnum.DCTERMS), "language", null,
					languageCode, true, null, null), true);
		}

		if (description != null)
			writer.write(createTriple(graphPrefix, name, prefixList.getPrefix(PrefixEnum.DCTERMS), "description", null,
					description, true, null, null), true);

		if (homepage != null)
			writer.write(createTriple(graphPrefix, name, prefixList.getPrefix(PrefixEnum.FOAF), "homepage", null,
					"<" + homepage + ">", false, null, null), true);

		if (date != null)
			writer.write(
					createTriple(graphPrefix, name, prefixList.getPrefix(PrefixEnum.DCTERMS), "modified", date, null),
					true);

	}

	public void writeLiteralRelation(LiteralRelation relation) {

		startInstance();
		RDFWriter writer = getWriter(RDFWriterName.LITERAL_RELATIONS, true);

		String object = relation.getObject();

		getId(relation.getSubject());
		if (relation.getSubject().getId() == null) {
			System.out.println("Subject ID missing for literal relation.");
			return;
		}

		String relationId = "literal_relation_" + String.valueOf(this.literalRelationNo);
		this.literalRelationNo += 1;

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "type",
				prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "Relation", false, null, relation.getDataSet()),
				true);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, relation.getSubject().getId(), false, null, relation.getDataSet()), true);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "object", null,
				createLiteral(object, relation.getDataType(), relation.getLanguageCode()), false, null,
				relation.getDataSet()), true);

		if (relation.getProperties() == null) {
			if (relation.getPrefix() == null) {
				System.out.println("Missing prefix: " + relation.getSubject().getId() + "\t" + relation.getProperty()
						+ "\t" + relation.getObject());
			}
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM), "roleType",
					relation.getPrefix(), relation.getProperty(), false, null, relation.getDataSet()), true);
		} else {
			Set<String> coveredProperties = new HashSet<String>();
			for (SubProperty property : relation.getProperties()) {
				writer.write(
						createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM), "roleType",
								property.getPrefix(), property.getProperty(), false, null, property.getDataSet()),
						!coveredProperties.contains(property.getProperty()));
				coveredProperties.add(property.getProperty());
			}
		}

		if (relation.getStartTime() != null) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM),
					"hasBeginTimeStamp", null, OUTPUT_DATE_FORMAT.format(relation.getStartTime().getDate()), false,
					null, relation.getDataSet()), true);

			writer.write(createDateGranularityTriple(this.basePrefix, relationId,
					relation.getStartTime().getGranularity(), relation.getDataSet(), true), true);
		}
		if (relation.getEndTime() != null) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM),
					"hasEndTimeStamp", null, OUTPUT_DATE_FORMAT.format(relation.getEndTime().getDate()), false, null,
					relation.getDataSet()), true);

			writer.write(createDateGranularityTriple(this.basePrefix, relationId,
					relation.getEndTime().getGranularity(), relation.getDataSet(), false), true);
		}

		endInstance();
	}

	public void writeGenericEventRelation(GenericRelation relation) {
		startInstance();

		RDFWriter writer = getWriter(RDFWriterName.EVENT_RELATIONS, true);

		writeGenericRelation(writer, relation);

		endInstance();
	}

	public void writeGenericEntityRelation(GenericRelation relation) {

		startInstance();

		RDFWriter writer = null;
		if (relation.getStartTime() == null && relation.getEndTime() == null)
			writer = getWriter(RDFWriterName.ENTITY_RELATIONS, true);
		else
			writer = getWriter(RDFWriterName.TEMPORAL_ENTITY_RELATIONS, true);

		writeGenericRelation(writer, relation);

		endInstance();
	}

	private void writeGenericRelation(RDFWriter writer, GenericRelation relation) {

		String relationId = "relation_" + String.valueOf(this.genericRelationNo);
		this.genericRelationNo += 1;

		getId(relation.getSubject());
		if (relation.getSubject().getId() == null) {
			System.out.println("Subject ID missing for generic relation.");
			return;
		}

		getId(relation.getObject());
		if (relation.getObject().getId() == null) {
			System.out.println("Object ID missing for generic relation.");
			return;
		}

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "type",
				prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "Relation", false, null, relation.getDataSet()),
				true);
		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, relation.getSubject().getId(), false, null, relation.getDataSet()), true);
		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "object",
				this.basePrefix, relation.getObject().getId(), false, null, relation.getDataSet()), true);

		if (relation.getProperties() == null) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM), "roleType",
					relation.getPrefix(), relation.getProperty(), false, null, relation.getDataSet()), true);
		} else {
			Set<String> coveredProperties = new HashSet<String>();
			for (SubProperty property : relation.getProperties()) {
				writer.write(
						createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM), "roleType",
								property.getPrefix(), property.getProperty(), false, null, property.getDataSet()),
						!coveredProperties.contains(property.getProperty()));
				coveredProperties.add(property.getProperty());
			}
		}

		if (relation.getStartTime() != null) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM),
					"hasBeginTimeStamp", null, OUTPUT_DATE_FORMAT.format(relation.getStartTime().getDate()), false,
					null, relation.getDataSet()), true);
			writer.write(createDateGranularityTriple(this.basePrefix, relationId,
					relation.getStartTime().getGranularity(), relation.getDataSet(), true), true);
		}
		if (relation.getEndTime() != null) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.SEM),
					"hasEndTimeStamp", null, OUTPUT_DATE_FORMAT.format(relation.getEndTime().getDate()), false, null,
					relation.getDataSet()), true);
			writer.write(createDateGranularityTriple(this.basePrefix, relationId,
					relation.getEndTime().getGranularity(), relation.getDataSet(), false), true);
		}

	}

	public void writeTextEventLinkCount(LinksToCountNew linkCount) {
		RDFWriter writer = getWriter(RDFWriterName.TEXT_EVENT_LINK_COUNTS, false);

		writeLinkCount(writer, linkCount);
	}

	public void writeLinkCount(Entity entity, Entity targetEntity, Map<Language, Integer> counts) {

		RDFWriter writer = null;
		if (entity.isEvent() || targetEntity.isEvent())
			writer = getWriter(RDFWriterName.EVENT_LINK_COUNTS, false);
		else
			writer = getWriter(RDFWriterName.ENTITY_LINK_COUNTS, false);

		writeLinkCount(writer, entity, targetEntity, counts);
	}

	private void writeLinkCount(RDFWriter writer, LinksToCountNew linkCount) {
		String relationId = "link_text_relation_" + String.valueOf(this.textLinkRelationNo);
		this.textLinkRelationNo += 1;

		getId(linkCount.getSource());
		getId(linkCount.getTarget());

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "type",
				prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "LinkRelation", false, null, null), false);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, linkCount.getSource().getId(), false, null, null), false);
		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, linkCount.getTarget().getId(), false, null, null), false);

		for (DataSet dataSet : linkCount.getCounts().keySet()) {
			writer.write(
					createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "links",
							null,
							createLiteral(String.valueOf(linkCount.getCounts().get(dataSet)),
									LiteralDataType.NON_NEGATIVE_INTEGER),
							false, dataSet.getLanguage(), dataSet),
					false);
		}

	}

	private void writeLinkCount(RDFWriter writer, Entity entity, Entity targetEntity, Map<Language, Integer> counts) {
		String relationId = "link_relation_" + String.valueOf(this.linksToRelationNo);
		this.linksToRelationNo += 1;

		getId(entity);
		getId(targetEntity);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "type",
				prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "LinkRelation", false, null, null), false);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, entity.getId(), false, null, null), false);
		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "object",
				this.basePrefix, targetEntity.getId(), false, null, null), false);

		for (Language language : counts.keySet()) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
					"links", null,
					createLiteral(String.valueOf(counts.get(language)), LiteralDataType.NON_NEGATIVE_INTEGER), false,
					language, DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA)), false);
		}
	}

	public void writeTextEventActorTriple(Event event, Entity entity) {
		RDFWriter writer = getWriter(RDFWriterName.TEXT_EVENTS, true);

		getId(entity);

		writer.write(createTriple(this.basePrefix, event.getId(), prefixList.getPrefix(PrefixEnum.SEM), "hasActor",
				this.basePrefix, entity.getId(), false, null, null), true);
	}

	public EntityIdGenerator getIdGenerator() {
		return idGenerator;
	}

	public void writeCoMentionCount(Entity entity, Entity targetEntity, Map<Language, Integer> counts) {

		RDFWriter writer = null;
		if (entity.isEvent() || targetEntity.isEvent())
			writer = getWriter(RDFWriterName.EVENT_MENTION_COUNTS, false);
		else
			writer = getWriter(RDFWriterName.ENTITY_MENTION_COUNTS, false);

		String relationId = "mention_relation_" + String.valueOf(this.coMentionRelationNo);
		this.coMentionRelationNo += 1;

		getId(entity);
		getId(targetEntity);

		writer.write(
				createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "type",
						prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "CoMentionRelation", false, null, null),
				false);

		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "subject",
				this.basePrefix, entity.getId(), false, null, null), false);
		writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.RDF), "object",
				this.basePrefix, targetEntity.getId(), false, null, null), false);

		for (Language language : counts.keySet()) {
			writer.write(createTriple(this.basePrefix, relationId, prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA),
					"mentions", null,
					createLiteral(String.valueOf(counts.get(language)), LiteralDataType.NON_NEGATIVE_INTEGER), false,
					language, DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA)), false);
		}

	}

}
