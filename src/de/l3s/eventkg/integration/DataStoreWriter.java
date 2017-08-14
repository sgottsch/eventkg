package de.l3s.eventkg.integration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Label;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.Prefix;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DataStoreWriter {

	public static final boolean OLD_MODEL = false;

	private static final int NUMBER_OF_LINES_IN_PREVIEW = 50;
	private DataStore dataStore;
	private DataSets dataSets;

	private SimpleDateFormat standardFormat = new SimpleDateFormat("\"yyyy-MM-dd\"'^^xsd:date'");

	public DataStoreWriter() {
		this.dataStore = DataStore.getInstance();
		this.dataSets = DataSets.getInstance();
	}

	public void write() {
		System.out.println("writeDataSets");
		writeDataSets();
		System.out.println("writeEvents");
		writeEvents();
		System.out.println("writeEntities");
		writeEntities();
		System.out.println("writeBaseRelations");
		writeBaseRelations();
		System.out.println("writeOtherRelations");
		writeOtherRelations();
		System.out.println("writePropertyLabels");
		writePropertyLabels();
		System.out.println("writeLinkRelations");
		writeLinkRelations();
	}

	private void writePropertyLabels() {
		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_PROPERTY_LABELS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG_SCHEMA);
			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (PropertyLabel propertyLabel : dataStore.getPropertyLabels()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo,
						propertyLabel.getPrefix().getAbbr() + propertyLabel.getProperty(),
						Prefix.RDF.getAbbr() + "label", propertyLabel.getLabel(), true, propertyLabel.getDataSet(),
						propertyLabel.getLanguage());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}
	}

	private void writeDataSets() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_DATASETS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.VOID);
			prefixes.add(Prefix.FOAF);
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG_SCHEMA);
			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			for (DataSet dataSet : dataSets.getAllDataSets()) {
				writeTriple(writer, null, null, Prefix.EVENT_KG_GRAPH.getAbbr() + dataSet.getId(),
						Prefix.RDF.getAbbr() + "type", Prefix.FOAF.getAbbr() + "Dataset", false, null);
				writeTriple(writer, null, null, Prefix.EVENT_KG_GRAPH.getAbbr() + dataSet.getId(),
						Prefix.FOAF.getAbbr() + "homepage", dataSet.getUrl(), false, null);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void writeEvents() {

		String eventId = "event_";
		int eventNo = 0;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;

		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_WITH_TEXTS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG_SCHEMA);
			prefixes.add(Prefix.DCTERMS);
			prefixes.add(Prefix.WIKIDATA_ENTITY);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_EN);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_RU);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_PT);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_DE);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_FR);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (Event event : dataStore.getEvents()) {

				event.setId("<" + eventId + String.valueOf(eventNo) + ">");

				// if (event.getEventEntity() != null)
				// event.getEventEntity().setId("<" + eventId +
				// String.valueOf(eventNo) + ">");

				eventNo += 1;
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.RDF.getAbbr() + "type",
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "Event", false, null);

				if (event.getWikidataId() != null)
					writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + Prefix.WIKIDATA_ENTITY.getUrlPrefix() + event.getWikidataId() + ">", false,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				if (event.getUrls() != null) {
					for (String url : event.getUrls())
						writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.DCTERMS.getAbbr() + "relation",
								"<" + url + ">", false, null);
				}

				Prefix prefix = null;
				for (Language language : event.getWikipediaLabels().keySet()) {
					switch (language) {
					// TODO: Create a mapping from language to prefix
					case EN:
						prefix = Prefix.DBPEDIA_RESOURCE_EN;
						break;
					case DE:
						prefix = Prefix.DBPEDIA_RESOURCE_DE;
						break;
					case PT:
						prefix = Prefix.DBPEDIA_RESOURCE_PT;
						break;
					case RU:
						prefix = Prefix.DBPEDIA_RESOURCE_RU;
						break;
					case FR:
						prefix = Prefix.DBPEDIA_RESOURCE_FR;
						break;
					default:
						break;
					}
					writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + prefix.getUrlPrefix() + event.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.DBPEDIA));
				}
			}

			System.out.println("labels: " + dataStore.getLabels().size());
			lineNo = 0;
			for (Label label : dataStore.getLabels()) {
				if (label.getSubject().isEvent() || label.getSubject().getEventEntity() != null) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, label.getSubject().getEventEntity().getId(),
							Prefix.RDF.getAbbr() + "label", label.getLabel(), true, label.getDataSet(),
							label.getLanguage());
				}
			}

			System.out.println("aliases: " + dataStore.getAliases().size());
			lineNo = 0;
			for (Alias alias : dataStore.getAliases()) {
				if (alias.getSubject().isEvent() || alias.getSubject().getEventEntity() != null) {
					Event event = alias.getSubject().getEventEntity();
					if (event == null)
						event = (Event) alias.getSubject();
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.DCTERMS.getAbbr() + "alternative",
							alias.getLabel(), true, alias.getDataSet(), alias.getLanguage());
				}
			}

			System.out.println("descriptions: " + dataStore.getDescriptions().size());
			lineNo = 0;
			for (Description description : dataStore.getDescriptions()) {
				if (description.getSubject() == null)
					continue;
				if (description.getSubject().isEvent() || description.getSubject().getEventEntity() != null) {
					Event event = description.getSubject().getEventEntity();
					if (event == null)
						event = (Event) description.getSubject();
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, event.getId(), Prefix.DCTERMS.getAbbr() + "description",
							description.getLabel(), true, description.getDataSet(), description.getLanguage());
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

		String entityId = "entity_";
		int entityNo = 0;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_WITH_TEXTS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_WITH_TEXTS_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG_SCHEMA);
			prefixes.add(Prefix.DCTERMS);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_EN);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_RU);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_PT);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_DE);
			prefixes.add(Prefix.DBPEDIA_RESOURCE_FR);
			prefixes.add(Prefix.WIKIDATA_ENTITY);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;

			for (Entity entity : dataStore.getEntities()) {
				lineNo += 1;

				if (entity.isEvent() || entity.getEventEntity() != null)
					continue;

				entity.setId("<" + entityId + String.valueOf(entityNo) + ">");

				entityNo += 1;
				writeTriple(writer, writerPreview, lineNo, entity.getId(), Prefix.RDF.getAbbr() + "type",
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "Entity", false, null);

				if (entity.isLocation()) {
					writeTriple(writer, writerPreview, lineNo, entity.getId(), Prefix.RDF.getAbbr() + "type",
							Prefix.EVENT_KG_SCHEMA.getAbbr() + "Location", false, null);

					for (Entity subLocation : entity.getSubLocations()) {
						writeTriple(writer, writerPreview, lineNo, entity.getId(),
								Prefix.SCHEMA_ORG.getAbbr() + "subLocation",
								Prefix.EVENT_KG_RESOURCE.getAbbr() + subLocation.getId(), false, null);
					}
					for (Entity parentLocation : entity.getParentLocations()) {
						writeTriple(writer, writerPreview, lineNo, entity.getId(),
								Prefix.SCHEMA_ORG.getAbbr() + "parentLocation",
								Prefix.EVENT_KG_RESOURCE.getAbbr() + parentLocation.getId(), false, null);
					}

				}

				if (entity.getWikidataId() != null)
					writeTriple(writer, writerPreview, lineNo, entity.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + Prefix.WIKIDATA_ENTITY.getUrlPrefix() + entity.getWikidataId() + ">", false,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				Prefix prefix = null;

				for (Language language : entity.getWikipediaLabels().keySet()) {
					switch (language) {
					// TODO: Create a mapping from language to prefix
					case EN:
						prefix = Prefix.DBPEDIA_RESOURCE_EN;
						break;
					case DE:
						prefix = Prefix.DBPEDIA_RESOURCE_DE;
						break;
					case PT:
						prefix = Prefix.DBPEDIA_RESOURCE_PT;
						break;
					case RU:
						prefix = Prefix.DBPEDIA_RESOURCE_RU;
						break;
					case FR:
						prefix = Prefix.DBPEDIA_RESOURCE_FR;
						break;
					default:
						break;
					}
					writeTriple(writer, writerPreview, lineNo, entity.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + prefix.getUrlPrefix() + entity.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.DBPEDIA));
				}

			}

			lineNo = 0;
			for (Label label : dataStore.getLabels()) {
				if (!label.getSubject().isEvent() && label.getSubject().getEventEntity() == null) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, label.getSubject().getId(),
							Prefix.RDF.getAbbr() + "label", label.getLabel(), true, label.getDataSet(),
							label.getLanguage());
				}
			}

			lineNo = 0;
			for (Alias alias : dataStore.getAliases()) {
				if (!alias.getSubject().isEvent() && alias.getSubject().getEventEntity() == null) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, alias.getSubject().getId(),
							Prefix.DCTERMS.getAbbr() + "alternative", alias.getLabel(), true, alias.getDataSet(),
							alias.getLanguage());
				}
			}

			lineNo = 0;
			for (Description description : dataStore.getDescriptions()) {
				if (description.getSubject() != null && !description.getSubject().isEvent()
						&& description.getSubject().getEventEntity() == null) {
					lineNo += 1;
					writeTriple(writer, writerPreview, lineNo, description.getSubject().getId(),
							Prefix.DCTERMS.getAbbr() + "description", description.getLabel(), true,
							description.getDataSet(), description.getLanguage());
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

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.SCHEMA_ORG);
			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int lineNo = 0;
			for (Location location : dataStore.getLocations()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, location.getSubject().getId(),
						Prefix.SCHEMA_ORG.getAbbr() + "location", location.getLocation().getId(), false,
						location.getDataSet(), null);
			}

			lineNo = 0;
			for (StartTime startTime : dataStore.getStartTimes()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, startTime.getSubject().getId(),
						Prefix.SCHEMA_ORG.getAbbr() + "startTime", standardFormat.format(startTime.getStartTime()),
						false, startTime.getDataSet(), null);
			}

			lineNo = 0;
			for (EndTime endTime : dataStore.getEndTimes()) {
				lineNo += 1;
				writeTriple(writer, writerPreview, lineNo, endTime.getSubject().getId(),
						Prefix.SCHEMA_ORG.getAbbr() + "endTime", standardFormat.format(endTime.getEndTime()), false,
						endTime.getDataSet(), null);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeLinkRelations() {

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_LINK_RELATIOINS);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_LINK_RELATIOINS_PREVIEW);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			// prefixes.add(Prefix.RDF);
			// prefixes.add(Prefix.EVENT_KG);
			// prefixes.add(Prefix.XSD);
			prefixes.add(Prefix.EVENT_KG_SCHEMA);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int relationNo = 0;
			int lineNo = 0;
			for (GenericRelation relation : dataStore.getLinkRelations()) {
				lineNo += 1;
				String relationId = "<eventkg_link_relation_" + String.valueOf(relationNo) + ">";

				Entity object = relation.getObject();
				if (object.getEventEntity() != null)
					object = object.getEventEntity();

				writeTriple(writer, writerPreview, lineNo, relationId, Prefix.RDF.getAbbr() + "type",
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "Relation", false, relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "relationSubject", relation.getSubject().getId(), false,
						relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "relationObject", object.getId(), false,
						relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId, Prefix.EVENT_KG_SCHEMA.getAbbr() + "relation",
						relation.getPrefix().getAbbr() + relation.getProperty(), false, relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId, Prefix.EVENT_KG_SCHEMA.getAbbr() + "strength",
						"\"" + String.valueOf(relation.getWeight()) + "\"^^xsd:double", false, relation.getDataSet());

				relationNo += 1;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private void writeOtherRelations() {

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
			for (Prefix prefix : Prefix.values())
				prefixes.add(prefix);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			int relationNo = 0;
			int lineNo = 0;
			for (GenericRelation relation : dataStore.getGenericRelations()) {

				Entity object = relation.getObject();
				if (object.getEventEntity() != null)
					object = object.getEventEntity();

				lineNo += 1;
				String relationId = "<eventkg_relation_" + String.valueOf(relationNo) + ">";

				writeTriple(writer, writerPreview, lineNo, relationId, Prefix.RDF.getAbbr() + "type",
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "Relation", false, relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "relationSubject", relation.getSubject().getId(), false,
						relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId,
						Prefix.EVENT_KG_SCHEMA.getAbbr() + "relationObject", object.getId(), false,
						relation.getDataSet());
				writeTriple(writer, writerPreview, lineNo, relationId, Prefix.EVENT_KG_SCHEMA.getAbbr() + "relation",
						relation.getPrefix().getAbbr() + relation.getProperty(), false, relation.getDataSet());

				if (relation.getStartTime() != null)
					writeTriple(writer, writerPreview, lineNo, relationId, Prefix.SCHEMA_ORG.getAbbr() + "startTime",
							standardFormat.format(relation.getStartTime()), false, relation.getDataSet());
				if (relation.getEndTime() != null)
					writeTriple(writer, writerPreview, lineNo, relationId, Prefix.SCHEMA_ORG.getAbbr() + "endTime",
							standardFormat.format(relation.getEndTime()), false, relation.getDataSet());

				// if (relation.getPropertyLabels() != null) {
				// for (Language language :
				// relation.getPropertyLabels().keySet())
				// writeTriple(writer, writerPreview, lineNo, relationId,
				// Prefix.RDF.getAbbr() + "label",
				// relation.getPropertyLabels().get(language), true,
				// relation.getDataSet(), language);
				// }

				relationNo += 1;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

	private List<String> createIntro(List<Prefix> prefixes) {

		if (!prefixes.contains(Prefix.EVENT_KG_GRAPH))
			prefixes.add(Prefix.EVENT_KG_GRAPH);

		if (OLD_MODEL) {
			prefixes.remove(Prefix.EVENT_KG_GRAPH);
			prefixes.remove(Prefix.EVENT_KG_SCHEMA);
			prefixes.remove(Prefix.EVENT_KG_RESOURCE);
		} else
			prefixes.remove(Prefix.EVENT_KG_OLD);

		List<String> lines = new ArrayList<String>();

		lines.add("");
		for (Prefix prefix : prefixes) {

			// ignore base relation
			if (prefix == Prefix.EVENT_KG_RESOURCE)
				continue;

			lines.add(
					"@prefix" + Config.SEP + prefix.getAbbr() + " <" + prefix.getUrlPrefix() + ">" + Config.SEP + ".");
		}

		if (!OLD_MODEL)
			lines.add("@base" + Config.SEP + "<" + Prefix.EVENT_KG_RESOURCE.getUrlPrefix() + ">" + Config.SEP + ".");
		else
			lines.add("@base" + Config.SEP + "<" + Prefix.EVENT_KG_OLD.getUrlPrefix() + ">" + Config.SEP + ".");

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

	private void writeTriple(PrintWriter writer, PrintWriter writerPreview, Integer lineNo, String subject,
			String property, String object, boolean quoteObject, DataSet dataSet) {

		if (object == null)
			return;

		if (quoteObject) {
			object = object.replace("\\", "\\\\");
			object = object.replaceAll("\"", "\\\\\"");
			object = "\"" + object + "\"";
		}

		// if (quoteObject) {
		// // object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"";
		// // TODO: Check example "https://www.wikidata.org/wiki/Q859038"
		// object = object.replace("\\", "\\\\");
		//
		// object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"";
		// }

		String line = null;
		if (dataSet == null) {
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL;
		} else
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP + Prefix.EVENT_KG_GRAPH.getAbbr()
					+ dataSet.getId() + Config.SEP + "." + Config.NL;

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
			line = subject + Config.SEP + property + Config.SEP + object + Config.SEP + Prefix.EVENT_KG_GRAPH.getAbbr()
					+ dataSet.getId() + Config.SEP + "." + Config.NL;

		writer.write(line);
		if (writerPreview != null && lineNo <= NUMBER_OF_LINES_IN_PREVIEW)
			writerPreview.write(line);

	}

}
