package de.l3s.eventkg.integration.collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.source.dbpedia.DBpediaTypeLabelsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaTypesExtractor;
import de.l3s.eventkg.source.wikidata.WikidataTypeLabelsExtractor;
import de.l3s.eventkg.source.wikidata.WikidataTypesExtractor;
import de.l3s.eventkg.util.MapUtil;

public class TypesCollector extends Extractor {

	private List<Language> languages;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter dataStoreWriter;

	public TypesCollector(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("TypesWriter", Source.EVENT_KG, "Integrates types from DBpedia and Wikidata into DBpedia ontology.",
				languages);
		this.languages = languages;
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	@Override
	public void run() {

		PrefixList prefixList = PrefixList.getInstance();

		// load DBpedia class labels
		DBpediaTypeLabelsExtractor dbpediaTypeLabelsExtractor = new DBpediaTypeLabelsExtractor(this.languages,
				this.dataStoreWriter);
		dbpediaTypeLabelsExtractor.printInformation();
		dbpediaTypeLabelsExtractor.run();

		// load Wikidata classes
		System.out.println("Run WikidataTypesExtractor.");
		WikidataTypesExtractor wikidataTypesExtractor = new WikidataTypesExtractor(this.languages,
				this.wikidataIdMappings, this.dataStoreWriter);
		wikidataTypesExtractor.run();

		// load Wikidata class labels
		WikidataTypeLabelsExtractor wikidataTypeLabelsExtractor = new WikidataTypeLabelsExtractor(this.languages,
				wikidataTypesExtractor.getUsedTypes(), this.dataStoreWriter);
		wikidataTypeLabelsExtractor.printInformation();
		wikidataTypeLabelsExtractor.run();

		System.out.println("Run DBpediaTypesExtractor.");
		DBpediaTypesExtractor dbpediaTypesExtractor = new DBpediaTypesExtractor(this.languages, this.wikidataIdMappings,
				this.dataStoreWriter);
		dbpediaTypesExtractor.run();

		// integrate
		Set<Entity> entities = new HashSet<Entity>();
		entities.addAll(wikidataTypesExtractor.getTypesPerEntity().keySet());
		entities.addAll(dbpediaTypesExtractor.getTypesPerEntity().keySet());
		System.out.println(wikidataTypesExtractor.getTypesPerEntity().size() + " entities with types in Wikidata.");
		System.out.println(dbpediaTypesExtractor.getTypesPerEntity().size() + " entities with types in DBpedia.");
		System.out.println(entities.size() + " entities with types in at least one source.");
		Map<String, Integer> eventClasses = new HashMap<String, Integer>();

		for (Entity entity : entities) {
			this.dataStoreWriter.startInstance();

			Set<String> types = new HashSet<String>();

			if (dbpediaTypesExtractor.getTypesPerEntity().containsKey(entity))
				types.addAll(dbpediaTypesExtractor.getTypesPerEntity().get(entity));

			if (wikidataTypesExtractor.getTypesPerEntity().containsKey(entity)) {
				Set<String> wikidataTypes = new HashSet<String>();
				// add transitive closure of all types
				wikidataTypes.addAll(wikidataTypesExtractor.getTransitiveClasses(entity));

				for (String wikidataType : wikidataTypes) {
					if (dbpediaTypesExtractor.getWikidataToDBO().containsKey(wikidataType)) {
						types.addAll(dbpediaTypesExtractor.getWikidataToDBO().get(wikidataType));
					}
				}
			}

			// integrate

			types = dbpediaTypesExtractor.resolveTransitively(types);

			for (String type : types) {
				if (entity.isEvent()) {
					if (!eventClasses.containsKey(type))
						eventClasses.put(type, 1);
					else
						eventClasses.put(type, eventClasses.get(type) + 1);
				}

				this.dataStoreWriter.writeTypeTriple(entity, prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), type,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);
			}
			
			this.dataStoreWriter.endInstance();
		}

		Map<String, Integer> orderedTypes = MapUtil.sortByValue(eventClasses);
		for (String type : orderedTypes.keySet())
			System.out.println(type + " -> " + orderedTypes.get(type));

		System.out.println("Done.");
	}

}
