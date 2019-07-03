package de.l3s.eventkg.integration;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.FileType;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.Pipeline;
import de.l3s.eventkg.source.dbpedia.DBpediaTypesExtractor;
import de.l3s.eventkg.source.wikidata.WikidataTypesExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.MapUtil;

public class TypesWriter extends Extractor {

	private EventKGIdMappingLoader eventKGIdMapping;

	public TypesWriter(List<Language> languages) {
		super("TypesWriter", Source.EVENT_KG, "Integrates types from DBpedia and Wikidata into DBpedia ontology.",
				languages);
	}

	public static void main(String[] args) {
		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		TypesWriter extractor = new TypesWriter(languages);
		extractor.run();
	}

	@Override
	public void run() {

		Pipeline.initDataSets(this.languages);

		System.out.println("Load EventKG ID mapping.");
		this.eventKGIdMapping = new EventKGIdMappingLoader(false);
		this.eventKGIdMapping.initEventIdMapping();
		this.eventKGIdMapping.initEntityIdMapping();
		System.out.println("\tDone.");

		// load Wikidata classes
		System.out.println("Run WikidataTypesExtractor.");
		WikidataTypesExtractor wikidataTypesExtractor = new WikidataTypesExtractor(this.languages,
				this.eventKGIdMapping);
		wikidataTypesExtractor.run();

		System.out.println("Run DBpediaTypesExtractor.");
		DBpediaTypesExtractor dbpediaTypesExtractor = new DBpediaTypesExtractor(this.languages, this.eventKGIdMapping);
		dbpediaTypesExtractor.run();

		// integrate
		Set<String> entities = new HashSet<String>();
		entities.addAll(wikidataTypesExtractor.getTypesPerEntity().keySet());
		entities.addAll(dbpediaTypesExtractor.getTypesPerEntity().keySet());
		System.out.println(wikidataTypesExtractor.getTypesPerEntity().size() + " entities with types in Wikidata.");
		System.out.println(dbpediaTypesExtractor.getTypesPerEntity().size() + " entities with types in DBpedia.");
		System.out.println(entities.size() + " entities with types in at least one source.");

		PrefixList prefixList = PrefixList.getInstance();

		PrintWriter writer = null;
		PrintWriter writerPreview = null;
		Map<String, Integer> eventClasses = new HashMap<String, Integer>();
		int lineNo = 0;
		DataSet dataSet = DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG);

		FileType fileType = FileType.NQ;
		
		DataStoreWriter dataStoreWriter= new DataStoreWriter(languages);
		dataStoreWriter.initPrefixes();

		try {
			writer = FileLoader.getWriter(FileName.ALL_NQ_TYPES);
			writerPreview = FileLoader.getWriter(FileName.ALL_NQ_TYPES_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY));
			for (String line : dataStoreWriter.createIntro(prefixes, prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			for (String eventKGId : entities) {

				Set<String> types = new HashSet<String>();

				if (dbpediaTypesExtractor.getTypesPerEntity().containsKey(eventKGId))
					types.addAll(dbpediaTypesExtractor.getTypesPerEntity().get(eventKGId));

				if (wikidataTypesExtractor.getTypesPerEntity().containsKey(eventKGId)) {
					Set<String> wikidataTypes = new HashSet<String>();
					// add transitive closure of all types
					wikidataTypes.addAll(wikidataTypesExtractor.getTransitiveClasses(eventKGId));
					// for (String wikidataType :
					// wikidataTypesExtractor.getTypesPerEntity().get(eventKGId))
					// {
					// wikidataTypes.add(wikidataType);
					// if
					// (wikidataTypesExtractor.getParentClasses().containsKey(eventKGId))
					// wikidataTypes.addAll(wikidataTypesExtractor.getParentClasses().get(eventKGId));
					// }
					for (String wikidataType : wikidataTypes) {
						if (dbpediaTypesExtractor.getWikidataToDBO().containsKey(wikidataType)) {
							types.addAll(dbpediaTypesExtractor.getWikidataToDBO().get(wikidataType));
						}
					}
				}

				if (eventKGId.equals("event_437985"))
					System.out.println("event_437985: " + types);

				if (eventKGId.equals("<event_437985>"))
					System.out.println("<event_437985>: " + types);

				if (eventKGId.equals("entity_4956997"))
					System.out.println("entity_4956997: " + types);

				if (eventKGId.equals("<entity_4956997>"))
					System.out.println("<entity_4956997>: " + types);

				// integrate

				types = dbpediaTypesExtractor.resolveTransitively(types);

				for (String type : types) {
					if (eventKGId.contains("event")) {
						if (!eventClasses.containsKey(type))
							eventClasses.put(type, 1);
						else
							eventClasses.put(type, eventClasses.get(type) + 1);
					}

					dataStoreWriter.writeTriple(writer, writerPreview, lineNo, dataStoreWriter.getBasePrefix(),eventKGId,
							prefixList.getPrefix(PrefixEnum.RDF), "type",
							prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), type, false, dataSet, fileType);
				}
				lineNo += 1;

			}

			Map<String, Integer> orderedTypes = MapUtil.sortByValue(eventClasses);
			for (String type : orderedTypes.keySet())
				System.out.println(type + " -> " + orderedTypes.get(type));

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

		System.out.println("Done.");
	}

}
