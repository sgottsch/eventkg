package de.l3s.eventkg.integration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Label;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DataStoreWriter {

	private static final int NUMBER_OF_LINES_IN_PREVIEW = 50;
	private DataStore dataStore;
	private DataSets dataSets;

	private boolean generateIdsFromPreviousEventKG = false;

	private static SimpleDateFormat standardFormat = new SimpleDateFormat("\"yyyy-MM-dd\"'^^xsd:date'");

	private PrefixList prefixList;

	private List<Language> languages;

	private Set<String> propertiesUsedInNamedEventRelations = new HashSet<String>();
	private int relationNo;
	private EntityIdGenerator idGenerator;

	public DataStoreWriter(List<Language> languages) {
		this.dataStore = DataStore.getInstance();
		this.dataSets = DataSets.getInstance();
		this.languages = languages;
	}

	public void write() {
		init();
		System.out.println("Write meta files.");
		writeMetaFiles();
		System.out.println("writeEvents");
		writeEvents();
		System.out.println("writeEntities");
		writeEntities();
		System.out.println("writeBaseRelations");
		writeBaseRelations();
		System.out.println("writeNamedEventRelations");
		writeNamedEventRelations();
		System.out.println("writeOtherRelations");
		writeOtherRelations();
		System.out.println("writePropertyLabels");
		writePropertyLabels();
		System.out.println("writeLinkRelations");
		writeLinkRelations();
	}

	private void writeMetaFiles() {
		writeDataSetDescriptions();
		copySchemaFile();
		createVoIDFile();
	}

	private void init() {

		prefixList = PrefixList.getInstance();

		if (generateIdsFromPreviousEventKG) {
			this.idGenerator = new EntityIdGenerator();
			this.idGenerator.load();
		}
	}

	private void writePropertyLabels() {
		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDFS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_PROPERTY));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (PropertyLabel propertyLabel : dataStore.getPropertyLabels()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo,
						propertyLabel.getPrefix().getAbbr() + propertyLabel.getProperty(),
						PrefixEnum.RDFS.getAbbr() + "label", propertyLabel.getLabel(), true, propertyLabel.getDataSet(),
						propertyLabel.getLanguage());
				if (propertiesUsedInNamedEventRelations.contains(propertyLabel.getProperty()))
					writeTriple(writer, writerPreview, lineNo,
							propertyLabel.getPrefix().getAbbr() + propertyLabel.getProperty(),
							PrefixEnum.RDF.getAbbr() + "type",
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "RoleType", true,
							propertyLabel.getDataSet(), propertyLabel.getLanguage());

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}
	}

	private void writeDataSetDescriptions() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_DATASETS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.VOID));
			prefixes.add(prefixList.getPrefix(PrefixEnum.FOAF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DCTERMS));
			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
			}

			for (DataSet dataSet : dataSets.getAllDataSets()) {
				writeTriple(writer, null, null, PrefixEnum.EVENT_KG_GRAPH.getAbbr() + dataSet.getId(),
						prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "type", PrefixEnum.FOAF.getAbbr() + "Dataset",
						false, null);
				writeTriple(writer, null, null, PrefixEnum.EVENT_KG_GRAPH.getAbbr() + dataSet.getId(),
						prefixList.getPrefix(PrefixEnum.FOAF).getAbbr() + "homepage", dataSet.getUrl(), false, null);
				if (dataSet.getDate() != null)
					writeTriple(writer, null, null, PrefixEnum.EVENT_KG_GRAPH.getAbbr() + dataSet.getId(),
							prefixList.getPrefix(PrefixEnum.DCTERMS).getAbbr() + "created",
							standardFormat.format(dataSet.getDate()), false, null);
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

	private void createVoIDFile() {

		String currentDate = standardFormat.format(new Date());

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_VOID);

			for (String line : FileLoader.readLines(FileName.ALL_TTL_VOID_INPUT)) {
				writer.write(line.replace("@creation_date@", currentDate) + Config.NL);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeEvents() {

		int eventNo = 0;

		if (generateIdsFromPreviousEventKG)
			eventNo = this.idGenerator.getLastEventNo() + 1;

		Map<Event, Map<DataSet, Set<String>>> descriptionMap = new HashMap<Event, Map<DataSet, Set<String>>>();

		for (Description description : dataStore.getDescriptions()) {
			if (description.getSubject() == null)
				continue;
			if (description.getSubject().isEvent()) {
				Event event = (Event) description.getSubject();
				if (event == null)
					event = (Event) description.getSubject();
				if (!descriptionMap.containsKey(event))
					descriptionMap.put(event, new HashMap<DataSet, Set<String>>());
				if (!descriptionMap.get(event).containsKey(description.getDataSet()))
					descriptionMap.get(event).put(description.getDataSet(), new HashSet<String>());

				descriptionMap.get(event).get(description.getDataSet()).add(description.getLabel());
			}
		}

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

			for (Language language : this.languages)
				prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language));

			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (Event event : dataStore.getEvents()) {

				// event.setId("<" + eventId + String.valueOf(eventNo) + ">");

				// if (event.getEventEntity() != null)
				// event.getEventEntity().setId("<" + eventId +
				// String.valueOf(eventNo) + ">");

				// eventNo += 1;

				String eventId = generateEventId(event, descriptionMap.get(event));
				if (eventId == null) {
					eventId = "<event_" + String.valueOf(eventNo) + ">";
					eventNo += 1;
				}
				event.setId(eventId);

				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, event.getId(), PrefixEnum.RDF.getAbbr() + "type",
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "Event", false,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG));

				if (event.getWikidataId() != null)
					writeTriple(writer, writerPreview, lineNo, event.getId(), PrefixEnum.OWL.getAbbr() + "sameAs",
							"<" + prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY).getUrlPrefix()
									+ event.getWikidataId() + ">",
							false, dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				if (event.getOtherUrls() != null) {
					for (DataSet dataSet : event.getOtherUrls().keySet()) {
						for (String otherUrl : event.getOtherUrls().get(dataSet)) {
							writeTriple(writer, writerPreview, lineNo, event.getId(),
									prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getAbbr() + "extractedFrom",
									"<" + otherUrl + ">", false, dataSet);
						}
					}
				}

				for (Language language : event.getWikipediaLabels().keySet()) {
					Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
					writeTriple(writer, writerPreview, lineNo, event.getId(), PrefixEnum.OWL.getAbbr() + "sameAs",
							"<" + prefix.getUrlPrefix() + event.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.DBPEDIA));
				}
			}

			System.out.println("#Wikipedia labels: " + dataStore.getWikipediaLabels().size() + ".");
			lineNo = 0;
			for (Label label : dataStore.getWikipediaLabels()) {
				if (label.getSubject().isEvent()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, label.getSubject().getId(),
							prefixList.getPrefix(PrefixEnum.RDFS).getAbbr() + "label",
							label.getLabel().replaceAll("_", " "), true, label.getDataSet(), label.getLanguage());
				}
			}

			System.out.println("#Wikidata labels: " + dataStore.getWikidataLabels().size() + ".");
			lineNo = 0;
			for (Label label : dataStore.getWikidataLabels()) {
				if (label.getSubject().isEvent()) {
					Event event = (Event) label.getSubject();
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(),
							prefixList.getPrefix(PrefixEnum.RDFS).getAbbr() + "label", label.getLabel(), true,
							label.getDataSet(), label.getLanguage());
				}
			}

			System.out.println("#aliases: " + dataStore.getAliases().size() + ".");
			lineNo = 0;
			for (Alias alias : dataStore.getAliases()) {
				if (alias.getSubject().isEvent()) {
					Event event = (Event) alias.getSubject();
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(),
							prefixList.getPrefix(PrefixEnum.DCTERMS).getAbbr() + "alternative", alias.getLabel(), true,
							alias.getDataSet(), alias.getLanguage());
				}
			}

			System.out.println("#descriptions: " + dataStore.getDescriptions().size() + ".");
			lineNo = 0;
			for (Description description : dataStore.getDescriptions()) {
				if (description.getSubject() == null)
					continue;
				if (description.getSubject().isEvent()) {
					Event event = (Event) description.getSubject();
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(),
							prefixList.getPrefix(PrefixEnum.DCTERMS).getAbbr() + "description", description.getLabel(),
							true, description.getDataSet(), description.getLanguage());
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeEntities() {

		// String entityId = "entity_";
		int entityNo = 0;

		if (generateIdsFromPreviousEventKG)
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
			prefixes.add(prefixList.getPrefix(PrefixEnum.OWL));
			for (Language language : this.languages)
				prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY));

			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;

			System.out.println("#entities: " + dataStore.getEntities().size());

			for (Entity entity : dataStore.getEntities()) {
				lineNo += 1;

				if (entity.isEvent())
					continue;

				// entity.setId("<" + entityId + String.valueOf(entityNo) +
				// ">");

				String entityId = generateEntityId(entity);
				if (entityId == null) {
					entityId = "<entity_" + String.valueOf(entityNo) + ">";
					entityNo += 1;
				}
				entity.setId(entityId);

				if (entity.isActor())
					writeTriple(writer, writerPreview, lineNo, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "type",
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "Actor", false, null);

				if (entity.isLocation())
					writeTriple(writer, writerPreview, lineNo, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "type",
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "Place", false, null);

				if (!entity.isActor() && !entity.isLocation())
					writeTriple(writer, writerPreview, lineNo, entity.getId(),
							prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "type",
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "Core", false, null);

				if (entity.getWikidataId() != null)
					writeTriple(writer, writerPreview, lineNo, entity.getId(), PrefixEnum.OWL.getAbbr() + "sameAs",
							"<" + prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY).getUrlPrefix()
									+ entity.getWikidataId() + ">",
							false, dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				for (Language language : entity.getWikipediaLabels().keySet()) {
					Prefix prefix = prefixList.getPrefix(PrefixEnum.DBPEDIA_RESOURCE, language);
					writeTriple(writer, writerPreview, lineNo, entity.getId(), PrefixEnum.OWL.getAbbr() + "sameAs",
							"<" + prefix.getUrlPrefix() + entity.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.DBPEDIA));
				}

			}

			lineNo = 0;
			for (Label label : dataStore.getWikipediaLabels()) {
				if (!label.getSubject().isEvent()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, label.getSubject().getId(),
							prefixList.getPrefix(PrefixEnum.RDFS).getAbbr() + "label",
							label.getLabel().replaceAll("_", " "), true, label.getDataSet(), label.getLanguage());
				}
			}

			lineNo = 0;
			for (Label label : dataStore.getWikidataLabels()) {
				if (!label.getSubject().isEvent()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, label.getSubject().getId(),
							prefixList.getPrefix(PrefixEnum.RDFS).getAbbr() + "label", label.getLabel(), true,
							label.getDataSet(), label.getLanguage());
				}
			}

			// lineNo = 0;
			// for (Alias alias : dataStore.getAliases()) {
			// if (!alias.getSubject().isEvent() &&
			// alias.getSubject().getEventEntity() == null) {
			// lineNo += 1;
			// writeTriple(writer, writerPreview, lineNo,
			// alias.getSubject().getId(),
			// prefixList.getPrefix(PrefixEnum.DCTERMS).getAbbr() +
			// "alternative", alias.getLabel(), true,
			// alias.getDataSet(), alias.getLanguage());
			// }
			// }
			//
			// lineNo = 0;
			// for (Description description : dataStore.getDescriptions()) {
			// if (description.getSubject() != null &&
			// !description.getSubject().isEvent()
			// && description.getSubject().getEventEntity() == null) {
			// lineNo += 1;
			// writeTriple(writer, writerPreview, lineNo,
			// description.getSubject().getId(),
			// prefixList.getPrefix(PrefixEnum.DCTERMS).getAbbr() +
			// "description", description.getLabel(),
			// true, description.getDataSet(), description.getLanguage());
			// }
			// }

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private String generateEntityId(Entity entity) {

		if (!generateIdsFromPreviousEventKG)
			return null;

		Set<String> entityIds = new HashSet<String>();

		if (entity.getWikidataId() != null && this.idGenerator.getEntityLabelsMap()
				.containsKey(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA))) {
			entityIds.add(this.idGenerator.getEntityLabelsMap()
					.get(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA))
					.get(entity.getWikidataId()));
		}

		for (Language language : entity.getWikipediaLabels().keySet()) {
			if (entity.getWikidataId() != null && this.idGenerator.getEntityLabelsMap()
					.containsKey(DataSets.getInstance().getDataSet(language, Source.DBPEDIA))) {
				entityIds.add(this.idGenerator.getEntityLabelsMap()
						.get(DataSets.getInstance().getDataSet(language, Source.DBPEDIA))
						.get(entity.getWikipediaLabels().get(language)));
			}

		}

		entityIds.remove(null);
		if (entityIds.size() == 1) {
			for (String entityId : entityIds) {
				return entityId;
			}
		}

		return null;
	}

	private String generateEventId(Event event, Map<DataSet, Set<String>> descriptionMap) {

		if (!generateIdsFromPreviousEventKG)
			return null;

		Set<String> eventIds = new HashSet<String>();

		if (event.getWikidataId() == null && event.getWikipediaLabels().isEmpty()) {
			// Textual events
			for (DataSet dataSet : descriptionMap.keySet()) {
				if (this.idGenerator.getEventDescriptionsMap().containsKey(dataSet)) {
					for (String description : descriptionMap.get(dataSet)) {
						if (this.idGenerator.getEventDescriptionsMap().get(dataSet).containsKey(description))
							eventIds.add(this.idGenerator.getEventDescriptionsMap().get(dataSet).get(description));
					}
				}
			}
		} else {
			// Wikidata
			if (event.getWikidataId() != null && this.idGenerator.getEventLabelsMap()
					.containsKey(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA))) {
				eventIds.add(this.idGenerator.getEventLabelsMap()
						.get(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA))
						.get(event.getWikidataId()));
			}

			for (Language language : event.getWikipediaLabels().keySet()) {
				// DBpedia
				if (event.getWikidataId() != null && this.idGenerator.getEventLabelsMap()
						.containsKey(DataSets.getInstance().getDataSet(language, Source.DBPEDIA))) {
					eventIds.add(this.idGenerator.getEventLabelsMap()
							.get(DataSets.getInstance().getDataSet(language, Source.DBPEDIA))
							.get(event.getWikipediaLabels().get(language)));
				}

			}
		}

		eventIds.remove(null);
		if (eventIds.size() == 1) {
			for (String eventId : eventIds) {
				return eventId;
			}
		}

		return null;
	}

	private void writeBaseRelations() {

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.SCHEMA_ORG));
			prefixes.add(prefixList.getPrefix(PrefixEnum.SEM));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY));
			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (Location location : dataStore.getLocations()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, location.getSubject().getId(),
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasPlace", location.getLocation().getId(),
						false, location.getDataSet(), null);
			}

			lineNo = 0;
			for (StartTime startTime : dataStore.getStartTimes()) {

				if (startTime.getStartTime() == null) {
					// TODO: Why?
					System.out.println("Start time is null: " + startTime.getSubject().getId() + ", "
							+ startTime.getSubject().getWikidataId() + ", " + startTime.getDataSet());
					if (startTime.getDataSet() != null)
						System.out.println("   " + startTime.getDataSet().getId());
					continue;
				}

				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, startTime.getSubject().getId(),
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasBeginTimeStamp",
						standardFormat.format(startTime.getStartTime()), false, startTime.getDataSet(), null);
			}

			lineNo = 0;
			for (EndTime endTime : dataStore.getEndTimes()) {

				if (endTime.getEndTime() == null) {
					// TODO: Why?
					System.out.println("End time is null: " + endTime.getSubject().getId() + ", "
							+ endTime.getSubject().getWikidataId() + ", " + endTime.getDataSet());
					if (endTime.getDataSet() != null)
						System.out.println("   " + endTime.getDataSet().getId());
					continue;
				}

				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, endTime.getSubject().getId(),
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasEndTimeStamp",
						standardFormat.format(endTime.getEndTime()), false, endTime.getDataSet(), null);
			}

			lineNo = 0;
			for (Entity entity : dataStore.getEntities()) {

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
							writeTriple(writer, writerPreview, lineNo, entity.getId(),
									prefixList.getPrefix(PrefixEnum.SCHEMA_ORG).getAbbr() + "containedInPlace",
									parentLocation.getId(), false, null);
						}

					}

				}
			}

			lineNo = 0;
			for (Event event : dataStore.getEvents()) {

				int lineNoPlusOne = lineNo + 1;

				for (Event parentEvent : event.getParents()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, parentEvent.getId(),
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasSubEvent", event.getId(), false, null);
				}

				for (Event nextEvent : event.getNextEvents()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(),
							prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY).getAbbr() + "nextEvent",
							nextEvent.getId(), false, null);
				}

				for (Event previousEvent : event.getPreviousEvents()) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(),
							prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY).getAbbr() + "previousEvent",
							previousEvent.getId(), false, null);
				}

				lineNo = Math.min(lineNo, lineNoPlusOne);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeLinkRelations() {

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
			// prefixes.add(Prefix.XSD);
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));

			for (String line : createIntro(prefixes, this.prefixList)) {
				writerEventLinks.write(line + Config.NL);
				writerEventLinksPreview.write(line + Config.NL);
				writerEntityLinks.write(line + Config.NL);
				writerEntityLinksPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			int lineNoEvents = 0;
			int lineNoEntities = 0;

			System.out.println("Link subjects: " + dataStore.getLinkRelationsBySubjectAndObject().keySet().size());

			for (Entity subject : dataStore.getLinkRelationsBySubjectAndObject().keySet()) {
				for (Entity object : dataStore.getLinkRelationsBySubjectAndObject().get(subject).keySet()) {

					boolean isEntityRelation = true;

					String subjectId = subject.getId();
					if (subject.isEvent()) {
						isEntityRelation = false;
					}
					if (subject.isEvent())
						isEntityRelation = false;

					String objectId = object.getId();
					if (object.isEvent()) {
						isEntityRelation = false;
					}
					if (object.isEvent())
						isEntityRelation = false;

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

					String relationId = "<eventkg_link_relation_" + String.valueOf(lineNo) + ">";

					writeTriple(writer, writerPreview, lineNoWriter, relationId, PrefixEnum.RDF.getAbbr() + "type",
							prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getAbbr() + "Relation", false, null);
					writeTriple(writer, writerPreview, lineNoWriter, relationId,
							prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "subject", subjectId, false, null);
					writeTriple(writer, writerPreview, lineNoWriter, relationId,
							prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "object", objectId, false, null);

					for (GenericRelation relation : dataStore.getLinkRelationsBySubjectAndObject().get(subject)
							.get(object)) {
						writeTriple(writer, writerPreview, lineNoWriter, relationId,
								relation.getPrefix().getAbbr() + relation.getProperty(),
								"\"" + String.valueOf(relation.getWeight().intValue()) + "\"^^xsd:nonNegativeInteger",
								false, relation.getDataSet());
					}
				}
			}

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

			for (String line : createIntro(prefixes, this.prefixList)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			this.relationNo = 0;

			for (GenericRelation relation : dataStore.getGenericRelations()) {

				if (!relation.getSubject().isEvent() && !relation.getObject().isEvent())
					continue;

				String relationId = "<eventkg_relation_" + String.valueOf(this.relationNo) + ">";
				this.relationNo += 1;

				writeTriple(writer, writerPreview, this.relationNo, relationId, PrefixEnum.RDF.getAbbr() + "type",
						prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getAbbr() + "Relation", false,
						relation.getDataSet());

				writeTriple(writer, writerPreview, this.relationNo, relationId,
						prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "subject", relation.getSubject().getId(),
						false, relation.getDataSet());

				writeTriple(writer, writerPreview, this.relationNo, relationId,
						prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "object", relation.getObject().getId(), false,
						relation.getDataSet());

				// writeTriple(writer, writerPreview, lineNo, relationId,
				// prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "value",
				// object.getId(), false,
				// relation.getDataSet());

				// if (object.isEvent())
				// writeTriple(writer, writerPreview, lineNo,
				// relation.getSubject().getId(),
				// prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasEvent",
				// relationId, false,
				// relation.getDataSet());
				// else
				// writeTriple(writer, writerPreview, lineNo,
				// relation.getSubject().getId(),
				// prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasActor",
				// relationId, false,
				// relation.getDataSet());

				writeTriple(writer, writerPreview, this.relationNo, relationId,
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "roleType",
						relation.getPrefix().getAbbr() + relation.getProperty(), false, relation.getDataSet());

				if (relation.getStartTime() != null)
					writeTriple(writer, writerPreview, this.relationNo, relationId,
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasBeginTimeStamp",
							standardFormat.format(relation.getStartTime()), false, relation.getDataSet());
				if (relation.getEndTime() != null)
					writeTriple(writer, writerPreview, this.relationNo, relationId,
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasEndTimeStamp",
							standardFormat.format(relation.getEndTime()), false, relation.getDataSet());

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

		this.relationNo += 1;

	}

	private void writeOtherRelations() {

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

			for (String line : createIntro(prefixes, this.prefixList)) {
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
				String relationId = "<eventkg_relation_" + String.valueOf(this.relationNo) + ">";
				this.relationNo += 1;

				writeTriple(writer, writerPreview, lineNo, relationId, PrefixEnum.RDF.getAbbr() + "type",
						prefixList.getPrefix(PrefixEnum.EVENT_KG_SCHEMA).getAbbr() + "Relation", false,
						relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "subject", relation.getSubject().getId(),
						false, relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						prefixList.getPrefix(PrefixEnum.RDF).getAbbr() + "object", object.getId(), false,
						relation.getDataSet());

				writeTriple(writer, writerPreview, lineNo, relationId,
						prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "roleType",
						relation.getPrefix().getAbbr() + relation.getProperty(), false, relation.getDataSet());

				if (relation.getStartTime() != null)
					writeTriple(writer, writerPreview, lineNo, relationId,
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasBeginTimeStamp",
							standardFormat.format(relation.getStartTime()), false, relation.getDataSet());
				if (relation.getEndTime() != null)
					writeTriple(writer, writerPreview, lineNo, relationId,
							prefixList.getPrefix(PrefixEnum.SEM).getAbbr() + "hasEndTimeStamp",
							standardFormat.format(relation.getEndTime()), false, relation.getDataSet());

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

	public static List<String> createIntro(List<Prefix> prefixes, PrefixList prefixList) {

		if (!prefixes.contains(PrefixEnum.EVENT_KG_GRAPH))
			prefixes.add(prefixList.getPrefix(PrefixEnum.EVENT_KG_GRAPH));

		List<String> lines = new ArrayList<String>();

		lines.add("");
		for (Prefix prefix : prefixes) {

			// ignore base relation
			if (prefix.getPrefixEnum() == PrefixEnum.EVENT_KG_RESOURCE)
				continue;

			lines.add(
					"@prefix" + Config.SEP + prefix.getAbbr() + " <" + prefix.getUrlPrefix() + ">" + Config.SEP + ".");
		}

		lines.add("@base" + Config.SEP + "<" + prefixList.getPrefix(PrefixEnum.EVENT_KG_RESOURCE).getUrlPrefix() + ">"
				+ Config.SEP + ".");

		lines.add("");

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

	public static void writeTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo, String subject,
			String property, String object, boolean quoteObject, DataSet dataSet) {

		if (object == null)
			return;

		if (quoteObject) {
			object = object.replace("\\", "\\\\");
			object = object.replaceAll("\"", "\\\\\"");
			object = "\"" + object + "\"";
		}

		String line = null;
		if (dataSet == null) {
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL;
		} else
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP
					+ PrefixEnum.EVENT_KG_GRAPH.getAbbr() + dataSet.getId() + Config.SEP + "." + Config.NL;

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

	private void writeTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo, String subject,
			String property, String object, boolean quoteObject, DataSet dataSet, Language language) {

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

		if (dataSet == null) {
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL;
		} else
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP
					+ PrefixEnum.EVENT_KG_GRAPH.getAbbr() + dataSet.getId() + Config.SEP + "." + Config.NL;

		writer.write(line);
		if (writerPreview != null && lineNo <= NUMBER_OF_LINES_IN_PREVIEW)
			writerPreview.write(line);

	}

}
