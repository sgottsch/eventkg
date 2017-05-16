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
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DataStoreWriter {

	private DataStore dataStore;
	private DataSets dataSets;

	private SimpleDateFormat standardFormat = new SimpleDateFormat("\"yyyy-MM-dd\"'^^xsd:date'");

	public DataStoreWriter() {
		this.dataStore = DataStore.getInstance();
		this.dataSets = DataSets.getInstance();
	}

	public void write(String path) {
		System.out.println("writeDataSets");
		writeDataSets();
		System.out.println("writeEvents");
		writeEvents();
		System.out.println("writeEntities");
		writeEntities();
		System.out.println("writeBaseRelations");
		writeBaseRelations();
		System.out.println("writeLinkRelations");
		writeLinkRelations();
		System.out.println("writeOtherRelations");
		writeOtherRelations();
	}

	private void writeDataSets() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_DATASETS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.VOID);
			prefixes.add(Prefix.FOAF);
			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			for (DataSet dataSet : dataSets.getAllDataSets()) {
				writeTriple(writer, dataSet.getId(), Prefix.RDF.getAbbr() + "type", Prefix.FOAF.getAbbr() + "Dataset",
						false, null);
				writeTriple(writer, dataSet.getId(), Prefix.FOAF.getAbbr() + "homepage", dataSet.getUrl(), false, null);
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
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_WITH_TEXTS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG);
			prefixes.add(Prefix.DCTERMS);
			prefixes.add(Prefix.WIKIDATA);
			prefixes.add(Prefix.DBPEDIA_EN);
			prefixes.add(Prefix.DBPEDIA_RU);
			prefixes.add(Prefix.DBPEDIA_PT);
			prefixes.add(Prefix.DBPEDIA_DE);
			prefixes.add(Prefix.DBPEDIA_FR);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			for (Event event : dataStore.getEvents()) {
				event.setId("<" + eventId + String.valueOf(eventNo) + ">");
				eventNo += 1;
				writeTriple(writer, event.getId(), Prefix.RDF.getAbbr() + "type", Prefix.EVENT_KG.getAbbr() + "Event",
						false, null);

				if (event.getWikidataId() != null)
					writeTriple(writer, event.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + Prefix.WIKIDATA.getUrlPrefix() + event.getWikidataId() + ">", false,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				if (event.getUrls() != null) {
					for (String url : event.getUrls())
						writeTriple(writer, event.getId(), Prefix.DCTERMS.getAbbr() + "relation", "<" + url + ">",
								false, null);
				}

				for (Language language : event.getWikipediaLabels().keySet()) {
					String dbPediaId = "";
					switch (language) {
					// TODO: Create a mapping from language to prefix
					case EN:
						dbPediaId = Prefix.DBPEDIA_EN.getUrlPrefix();
						break;
					case DE:
						dbPediaId = Prefix.DBPEDIA_DE.getUrlPrefix();
						break;
					case PT:
						dbPediaId = Prefix.DBPEDIA_PT.getUrlPrefix();
						break;
					case RU:
						dbPediaId = Prefix.DBPEDIA_RU.getUrlPrefix();
						break;
					case FR:
						dbPediaId = Prefix.DBPEDIA_FR.getUrlPrefix();
						break;
					default:
						break;
					}
					writeTriple(writer, event.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + dbPediaId + event.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.WIKIPEDIA));
				}
			}

			System.out.println("labels: " + dataStore.getLabels().size());
			for (Label label : dataStore.getLabels()) {
				if (label.getSubject().isEvent() || label.getSubject().getEventEntity() != null) {
					writeTriple(writer, label.getSubject().getEventEntity().getId(), Prefix.RDF.getAbbr() + "label",
							label.getLabel(), true, label.getDataSet(), label.getLanguage());
				}
			}

			System.out.println("aliases: " + dataStore.getAliases().size());
			for (Alias alias : dataStore.getAliases()) {
				if (alias.getSubject().isEvent() || alias.getSubject().getEventEntity() != null) {
					Event event = alias.getSubject().getEventEntity();
					if (event == null)
						event = (Event) alias.getSubject();
					writeTriple(writer, event.getId(), Prefix.DCTERMS.getAbbr() + "alternative", alias.getLabel(), true,
							alias.getDataSet(), alias.getLanguage());
				}
			}

			System.out.println("descriptions: " + dataStore.getDescriptions().size());
			for (Description description : dataStore.getDescriptions()) {
				if (description.getSubject() == null)
					continue;
				if (description.getSubject().isEvent() || description.getSubject().getEventEntity() != null) {
					Event event = description.getSubject().getEventEntity();
					if (event == null)
						event = (Event) description.getSubject();
					writeTriple(writer, event.getId(), Prefix.DCTERMS.getAbbr() + "description", description.getLabel(),
							true, description.getDataSet(), description.getLanguage());
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeEntities() {

		String entityId = "entity_";
		int entityNo = 0;

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_ENTITIES_WITH_TEXTS);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG);
			prefixes.add(Prefix.DCTERMS);
			prefixes.add(Prefix.DBPEDIA_EN);
			prefixes.add(Prefix.DBPEDIA_RU);
			prefixes.add(Prefix.DBPEDIA_PT);
			prefixes.add(Prefix.DBPEDIA_DE);
			prefixes.add(Prefix.DBPEDIA_FR);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			for (Entity entity : dataStore.getEntities()) {

				if (entity.isEvent() || entity.getEventEntity() != null)
					continue;

				entity.setId("<" + entityId + String.valueOf(entityNo) + ">");
				entityNo += 1;
				writeTriple(writer, entity.getId(), Prefix.RDF.getAbbr() + "type", Prefix.EVENT_KG.getAbbr() + "Entity",
						false, null);

				if (entity.getWikidataId() != null)
					writeTriple(writer, entity.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + Prefix.WIKIDATA.getAbbr() + entity.getWikidataId() + ">", false,
							dataSets.getDataSetWithoutLanguage(Source.WIKIDATA));

				for (Language language : entity.getWikipediaLabels().keySet()) {
					String dbPediaId = "";
					switch (language) {
					// TODO: Create a mapping from language to prefix
					case EN:
						dbPediaId = Prefix.DBPEDIA_EN.getUrlPrefix();
						break;
					case DE:
						dbPediaId = Prefix.DBPEDIA_DE.getUrlPrefix();
						break;
					case PT:
						dbPediaId = Prefix.DBPEDIA_PT.getUrlPrefix();
						break;
					case RU:
						dbPediaId = Prefix.DBPEDIA_RU.getUrlPrefix();
						break;
					case FR:
						dbPediaId = Prefix.DBPEDIA_FR.getUrlPrefix();
						break;
					default:
						break;
					}
					writeTriple(writer, entity.getId(), Prefix.DCTERMS.getAbbr() + "relation",
							"<" + dbPediaId + entity.getWikipediaLabels().get(language) + ">", false,
							dataSets.getDataSet(language, Source.WIKIPEDIA));
				}

			}

			for (Label label : dataStore.getLabels()) {
				if (!label.getSubject().isEvent() && label.getSubject().getEventEntity() == null) {
					writeTriple(writer, label.getSubject().getId(), Prefix.RDF.getAbbr() + "label", label.getLabel(),
							true, label.getDataSet(), label.getLanguage());
				}
			}

			for (Alias alias : dataStore.getAliases()) {
				if (!alias.getSubject().isEvent() && alias.getSubject().getEventEntity() == null) {
					writeTriple(writer, alias.getSubject().getId(), Prefix.DCTERMS.getAbbr() + "alternative",
							alias.getLabel(), true, alias.getDataSet(), alias.getLanguage());
				}
			}

			for (Description description : dataStore.getDescriptions()) {
				if (description.getSubject() != null && !description.getSubject().isEvent()
						&& description.getSubject().getEventEntity() == null) {
					writeTriple(writer, description.getSubject().getId(), Prefix.DCTERMS.getAbbr() + "description",
							description.getLabel(), true, description.getDataSet(), description.getLanguage());
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeBaseRelations() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.SCHEMA_ORG);
			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			for (Location location : dataStore.getLocations()) {
				writeTriple(writer, location.getSubject().getId(), Prefix.SCHEMA_ORG.getAbbr() + "location",
						location.getLocation().getId(), false, location.getDataSet(), null);
			}

			for (StartTime startTime : dataStore.getStartTimes()) {

				// TODO: Why null here?
				if (startTime.getStartTime() == null)
					continue;

				writeTriple(writer, startTime.getSubject().getId(), Prefix.SCHEMA_ORG.getAbbr() + "startTime",
						standardFormat.format(startTime.getStartTime()), false, startTime.getDataSet(), null);
			}

			for (EndTime endTime : dataStore.getEndTimes()) {
				if (endTime.getEndTime() == null)
					continue;
				writeTriple(writer, endTime.getSubject().getId(), Prefix.SCHEMA_ORG.getAbbr() + "endTime",
						standardFormat.format(endTime.getEndTime()), false, endTime.getDataSet(), null);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeLinkRelations() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_LINK_RELATIOINS);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.RDF);
			prefixes.add(Prefix.EVENT_KG);
			prefixes.add(Prefix.XSD);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			int relationNo = 0;
			for (GenericRelation relation : dataStore.getLinkRelations()) {
				String relationId = "eventkg_link_relation_" + String.valueOf(relationNo);

				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relationSubject",
						relation.getSubject().getId(), false, relation.getDataSet());
				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relationObject",
						relation.getObject().getId(), false, relation.getDataSet());
				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relation", relation.getProperty(), false,
						relation.getDataSet());
				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "strength",
						"\"" + String.valueOf(relation.getWeight()) + "\"^^xsd:double", false, relation.getDataSet());

				relationNo += 1;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeOtherRelations() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_EVENTS_OTHER_RELATIONS);
			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(Prefix.EVENT_KG);
			prefixes.add(Prefix.RDF);

			for (String line : createIntro(prefixes)) {
				writer.write(line + Config.NL);
			}

			int relationNo = 0;
			for (GenericRelation relation : dataStore.getGenericRelations()) {
				String relationId = "<eventkg_relation_" + String.valueOf(relationNo) + ">";

				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relationSubject",
						relation.getSubject().getId(), false, relation.getDataSet());
				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relationObject",
						relation.getObject().getId(), false, relation.getDataSet());
				writeTriple(writer, relationId, Prefix.EVENT_KG.getAbbr() + "relation", relation.getProperty(), false,
						relation.getDataSet());

				if (relation.getPropertyLabels() != null) {
					for (Language language : relation.getPropertyLabels().keySet())
						writeTriple(writer, relationId, Prefix.RDF.getAbbr() + "label",
								relation.getPropertyLabels().get(language), true, relation.getDataSet(), language);
				}

				relationNo += 1;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private List<String> createIntro(List<Prefix> prefixes) {

		List<String> lines = new ArrayList<String>();

		lines.add("");
		for (Prefix prefix : prefixes) {
			lines.add("@prefix " + prefix.getAbbr() + " " + prefix.getUrlPrefix() + Config.SEP + ".");
		}
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

	private void writeTriple(PrintWriter writer, String subject, String property, String object, boolean quoteObject,
			DataSet dataSet) {

		if (object == null)
			return;

		if (quoteObject) {
			object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"";
		}

		writer.write(subject + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL);
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

	private void writeTriple(PrintWriter writer, String subject, String property, String object, boolean quoteObject,
			DataSet dataSet, Language language) {

		if (object == null)
			return;

		if (quoteObject && language != null) {
			object = "\"" + object.replaceAll("\"", "\\\\\"") + "\"@" + language.getLanguageLowerCase();
		} else if (quoteObject)
			object = "\"" + object.replaceAll("\"", "\\\\\"");

		if (dataSet == null)
			writer.write(subject + Config.SEP + property + Config.SEP + object + Config.SEP + "." + Config.NL);
		else
			writer.write(subject + Config.SEP + property + Config.SEP + object + Config.SEP + dataSet.getId()
					+ Config.SEP + "." + Config.NL);

	}

}
