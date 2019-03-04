package de.l3s.eventkg.integration.collection;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.LiteralDataType;
import de.l3s.eventkg.integration.model.relation.LiteralDataTypeCollection;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikidata.WikidataSnakType;
import de.l3s.eventkg.source.wikidata.processors.RelationsToEventPagesProcessor;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class LiteralRelationsCollector extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;
	private Set<LiteralRelation> relations = new HashSet<LiteralRelation>();

	public LiteralRelationsCollector(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("LiteralRelationsCollector", Source.ALL,
				"Integrates literal relations from all sources (s.t. they use the same set of entities. Different relations are not merged.).",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {

		System.out.println("Load YAGO literal facts.");
		loadYAGO();
		System.out.println("# Literal relations with YAGO: " + this.relations.size() + ".");

		System.out.println("Load Wikidata literal facts.");
		loadWikidata();
		System.out.println("# Literal relations with Wikidata: " + this.relations.size() + ".");

		System.out.println("Load DBpedia literal facts.");
		loadDBpediaRelations();
		System.out.println("# Literal relations with DBpedia: " + this.relations.size() + ".");

		System.out.println("Collect triples with literals.");
		collectTriples();
		System.out
				.println("# Literal relations with all: " + DataStore.getInstance().getLiteralRelations().size() + ".");

	}

	private void loadYAGO() {

		Prefix prefix = PrefixList.getInstance().getPrefix(PrefixEnum.YAGO);
		System.out.println("YAGO: " + prefix.getAbbr());

		System.out.println("File: " + FileName.YAGO_EVENT_LITERALS_FACTS.getFileName());

		System.out.println("Path: " + FileLoader.getPath(FileName.YAGO_EVENT_LITERALS_FACTS));

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.YAGO_EVENT_LITERALS_FACTS);

			LiteralRelation previousRelation = null;
			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					String timeString = parts[2];

					if (parts[1].equals("<occursSince>")) {
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						DateWithGranularity date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
				} else {

					// e.g.: Ex_Machina_(comics) <hasISBN> "1401206263" .

					String property = parts[1];

					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
					yagoLabelExtractor1.extractLabel();
					if (!yagoLabelExtractor1.isValid())
						continue;

					Entity entity = buildEntity(yagoLabelExtractor1.getLanguage(),
							yagoLabelExtractor1.getWikipediaLabel());

					if (entity == null)
						continue;

					String object = parts[2];

					String dataTypeString = "";
					if (object.contains("^^")) {
						dataTypeString = object.substring(object.lastIndexOf("^") + 1);
						// remove " ." in the end
						dataTypeString = dataTypeString.replaceAll(" .$", "");
					}

					// object is everything between first " and last "
					object = object.substring(object.indexOf("\"") + 1, object.lastIndexOf("\""));
					if (object.isEmpty())
						continue;

					LiteralDataType dataType = LiteralDataTypeCollection.getInstance().getDataType(dataTypeString);

					if (dataType == null || !dataType.isUsed())
						continue;

					previousRelation = buildRelation(entity, object, null, null, property,
							DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO), Language.EN, dataType,
							prefix, null);
				}

			}

		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			it.close();
		}

	}

	private void loadWikidata() {

		Prefix prefix = PrefixList.getInstance().getPrefix(PrefixEnum.WIKIDATA_PROPERTY);
		System.out.println("Wikidata: " + prefix.getAbbr());

		Map<Entity, Map<String, Set<LiteralRelation>>> wikidataRelationsBySubjectAndProperty = new HashMap<Entity, Map<String, Set<LiteralRelation>>>();

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.WIKIDATA_EVENT_LITERALS_RELATIONS);
			while (it.hasNext()) {

				String line = it.nextLine();

				String[] parts = line.split("\t");
				String subject = parts[0];
				Entity entity = this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(subject);

				if (entity == null)
					continue;

				String property = parts[1];

				WikidataSnakType snakType = WikidataSnakType.valueOf(parts[2]);

				if (snakType != WikidataSnakType.QUANTITY && snakType != WikidataSnakType.STRING
						&& snakType != WikidataSnakType.MONOLINGUAL_TEXT)
					continue;

				String snak = parts[3];

				String[] snakParts = snak.split("\\" + RelationsToEventPagesProcessor.TAB2);
				String object = snakParts[0];

				StatementRank rank = StatementRank.valueOf(parts[4]);

				if (rank == StatementRank.DEPRECATED)
					continue;

				String dataTypeString = null;
				if (snakParts.length > 3)
					dataTypeString = snakParts[3];

				LiteralDataType dataType = null;
				if (dataTypeString == null) {
					if (snakType == WikidataSnakType.QUANTITY) {
						if (!object.contains("."))
							dataType = LiteralDataType.INTEGER;
						else
							dataType = LiteralDataType.DOUBLE;
					} else if (snakType == WikidataSnakType.MONOLINGUAL_TEXT)
						dataType = LiteralDataType.LANG_STRING;
					else if (snakType == WikidataSnakType.STRING)
						dataType = LiteralDataType.LANG_STRING;
				} else
					dataType = LiteralDataTypeCollection.getInstance().getDataType(dataTypeString);

				String languageCode = null;

				if (dataType == null || !dataType.isUsed())
					continue;

				if (!wikidataRelationsBySubjectAndProperty.containsKey(entity))
					wikidataRelationsBySubjectAndProperty.put(entity, new HashMap<String, Set<LiteralRelation>>());

				if (!wikidataRelationsBySubjectAndProperty.get(entity).containsKey(property))
					wikidataRelationsBySubjectAndProperty.get(entity).put(property, new HashSet<LiteralRelation>());

				if (snakType == WikidataSnakType.MONOLINGUAL_TEXT || snakType == WikidataSnakType.STRING) {
					// object = "\"" + object + "\"";
				}
				if (snakType == WikidataSnakType.MONOLINGUAL_TEXT) {
					// add language identifier
					// e.g. "Q27192 P1448 MONOLINGUAL_TEXT National Challenge
					// Cup|en NORMAL "
					if (!snakParts[1].isEmpty())
						languageCode = snakParts[1];
					// object += "@" + snakParts[1];
				}

				LiteralRelation relation = buildRelation(entity, object, null, null, property,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), Language.EN, dataType,
						prefix, languageCode);
				relation.setStatementRank(rank);
				wikidataRelationsBySubjectAndProperty.get(entity).get(property).add(relation);

			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			it.close();
		}

		// if the same subject and property has multiple values: only keep the
		// one with preferred rank. If there is only one value, the normal rank
		// is often used, which is okay.
		for (Entity entity : wikidataRelationsBySubjectAndProperty.keySet()) {
			for (String property : wikidataRelationsBySubjectAndProperty.get(entity).keySet()) {
				if (wikidataRelationsBySubjectAndProperty.get(entity).get(property).size() > 1) {
					for (LiteralRelation relation : wikidataRelationsBySubjectAndProperty.get(entity).get(property)) {
						if (relation.getStatementRank() != StatementRank.PREFERRED) {
							this.relations.remove(relation);
						}
					}
				}
			}
		}

	}

	private void loadDBpediaRelations() {

		Prefix prefix = PrefixList.getInstance().getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY);
		System.out.println("DBpedia: " + prefix.getAbbr());

		for (Language language : languages) {

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.DBPEDIA_EVENT_LITERAL_RELATIONS, language);

				while (it.hasNext()) {
					String line = it.nextLine();

					String[] parts = line.split("\t");
					String subject = parts[0];

					String property = parts[1];

					if (property.startsWith("<"))
						property = property.substring(1, property.length() - 1);

					property = property.replace("http://dbpedia.org/ontology/", "");

					Entity entity = buildEntity(language, subject);

					if (entity == null)
						continue;

					String object = parts[2];

					String dataTypeString = "";
					if (object.contains("^^")) {
						dataTypeString = object.substring(object.lastIndexOf("^") + 1);
						// remove " ." in the end
						dataTypeString = dataTypeString.replaceAll(" .$", "");
					}

					// extract language code
					String languageCode = null;
					if (object.matches(".*\"@.. \\.$")) {
						languageCode = object.substring(object.length() - 4, object.length() - 2);
					}

					// object is everything between first " and last "
					object = object.substring(object.indexOf("\"") + 1, object.lastIndexOf("\""));
					if (object.isEmpty())
						continue;

					LiteralDataType dataType = LiteralDataTypeCollection.getInstance().getDataType(dataTypeString);

					if (dataType == null || !dataType.isUsed())
						continue;

					buildRelation(entity, object, null, null, property,
							DataSets.getInstance().getDataSet(language, Source.DBPEDIA), language, dataType, prefix,
							languageCode);

				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				it.close();
			}
		}

	}

	private void collectTriples() {

		Set<String> wikidataRelationsWhoseLabelsWereStored = new HashSet<String>();

		for (PropertyLabel propertyLabel : DataStore.getInstance().getPropertyLabels()) {
			wikidataRelationsWhoseLabelsWereStored.add(propertyLabel.getProperty());
		}

		Prefix wikidataPrefix = PrefixList.getInstance().getPrefix(PrefixEnum.WIKIDATA_PROPERTY);

		for (LiteralRelation relation : this.relations) {

			DataStore.getInstance().addLiteralRelation(relation);

			// Wikidata: Collect property labels
			if (relation.getDataSet().getSource() == Source.WIKIDATA) {

				if (!wikidataRelationsWhoseLabelsWereStored.contains(relation.getProperty())) {
					for (Language language : this.languages) {
						if (allEventPagesDataSet.getWikidataIdMappings().getWikidataPropertysByID(language,
								relation.getProperty()) != null) {
							DataStore.getInstance()
									.addPropertyLabel(new PropertyLabel(wikidataPrefix, relation.getProperty(),
											allEventPagesDataSet.getWikidataIdMappings()
													.getWikidataPropertysByID(language, relation.getProperty()),
											language, relation.getDataSet()));
						}
					}
					wikidataRelationsWhoseLabelsWereStored.add(relation.getProperty());
				}
			}

		}

		System.out.println("Literal relations: " + DataStore.getInstance().getLiteralRelations().size());

		// for (LiteralRelation lr :
		// DataStore.getInstance().getLiteralRelations()) {
		// System.out.println(lr.getSubject().getWikidataId() + "\t" +
		// lr.getProperty() + "\t" + lr.getObject() + " - "
		// + lr.getDataSet().getId());
		// }

	}

	private Entity buildEntity(Language language, String wikipediaLabel) {
		return this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, wikipediaLabel);
	}

	private LiteralRelation buildRelation(Entity entity, String object, DateWithGranularity startTime,
			DateWithGranularity endTime, String property, DataSet dataSet, Language sourceLanguage,
			LiteralDataType dataType, Prefix prefix, String languageCode) {

		if (prefix == null) {
			System.out.println("Prefix of LiteralRelation is null (LiteralRelationsCollector.buildRelation).");
		}

		if (entity == null || object == null)
			return null;

		if (property.startsWith("http:") || property.startsWith("https:")) {
			property = "<" + property + ">";
			prefix = PrefixList.getInstance().getPrefix(PrefixEnum.NO_PREFIX);
		}

		LiteralRelation relation = new LiteralRelation(entity, object, languageCode, prefix, property, startTime,
				endTime, dataSet, dataType);

		this.relations.add(relation);

		return relation;
	}

}
