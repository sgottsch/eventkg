package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaTypesExtractor extends Extractor {

	private WikidataIdMappings wikidataIdMappings;

	private Map<Entity, Set<String>> typesPerEntity = new HashMap<Entity, Set<String>>();
	private Map<String, Set<String>> parentClasses = new HashMap<String, Set<String>>();

	private Map<String, Set<String>> wikidataToDBO = new HashMap<String, Set<String>>();

	private TriplesWriter datastoreWriter;

	public DBpediaTypesExtractor(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("DBpediaTypesExtractor", Source.DBPEDIA, "Loads all DBpedia:ontology types.", languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.datastoreWriter = dataStoreWriter;
	}

	public static void main(String[] args) {
		Map<String, Set<String>> parentClasses = parseOntology();

		System.out.println(parentClasses.get("FormulaOneRacer"));
	}

	@Override
	public void run() {
		PrefixList prefixList = PrefixList.getInstance();

		this.parentClasses = parseOntologyAndWriteSubclasses(this.datastoreWriter, prefixList, this.wikidataToDBO);

		for (Language language : languages) {

			Set<String> usedLines = new HashSet<String>();
			BufferedReader br = null;
			DataSet dataSet = DataSets.getInstance().getDataSet(language, Source.DBPEDIA);

			if (FileLoader.fileExists(FileName.DBPEDIA_TYPES, language)) {

				try {
					br = FileLoader.getReader(FileName.DBPEDIA_TYPES, language);
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#"))
							continue;

						String[] parts = line.split(" ");

						String type = parts[2];

						if (!type.startsWith("<http://dbpedia.org/ontology"))
							continue;
						type = type.substring(type.lastIndexOf("/") + 1, type.length() - 1);

						// manually solve bug in Russian DBpedia (many
						// "book" types)
						if (language == Language.RU && type.equals("Book"))
							continue;
						// manually solve bug in Germany DBpedia (many
						// "WrittenWork" types)
						if (language == Language.DE && type.equals("WrittenWork"))
							continue;

						// German DBpedia is also erroneous for dbo:Place.
						// For example, World War II is a place there
						if (language == Language.DE && type.equals("Place"))
							continue;

						String resource = parts[0];
						resource = resource.substring(resource.lastIndexOf("/") + 1, resource.length() - 1);
						if (resource.contains("__"))
							resource = resource.substring(0, resource.lastIndexOf("__"));

						Entity eventKGEntity = this.wikidataIdMappings.getEntityByWikipediaLabel(dataSet.getLanguage(),
								resource);
						if (eventKGEntity == null) {
							continue;
						}

						String eventKGId = eventKGEntity.getId();
						if (eventKGId == null) {
							continue;
						}

						String lineId = resource + " " + type;

						if (usedLines.contains(lineId))
							continue;
						usedLines.add(lineId);

						if (!typesPerEntity.containsKey(eventKGEntity))
							typesPerEntity.put(eventKGEntity, new HashSet<String>());

						typesPerEntity.get(eventKGEntity).add(type);

						datastoreWriter.startInstance();
						datastoreWriter.writeDBPediaTypeTriple(eventKGEntity,
								prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), type, dataSet, false);
						datastoreWriter.endInstance();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

	}

	public Set<String> resolveTransitively(Set<String> types) {
		Set<String> typesToRemove = new HashSet<String>();

		for (String type : types) {
			if (this.parentClasses.containsKey(type)) {
				typesToRemove.addAll(this.parentClasses.get(type));
			}
		}

		types.removeAll(typesToRemove);

		return types;
	}

	public static Map<String, Set<String>> parseOntology() {
		return parseOntologyAndWriteSubclasses(null, null, null);
	}

	private static Map<String, Set<String>> parseOntologyAndWriteSubclasses(TriplesWriter datastoreWriter,
			PrefixList prefixList, Map<String, Set<String>> wikidataToDBO) {

		Map<String, Set<String>> parentClasses = new HashMap<String, Set<String>>();

		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.DBPEDIA_ONTOLOGY);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");

				if (parts[1].equals("<http://www.w3.org/2002/07/owl#equivalentClass>")) {

					String type1 = parts[0];
					if (!type1.startsWith("<http://dbpedia.org/ontology"))
						continue;
					type1 = type1.substring(type1.lastIndexOf("/") + 1, type1.length() - 1);

					String wikidataType = parts[2];
					if (!wikidataType.startsWith("<http://www.wikidata.org/entity/Q"))
						continue;
					wikidataType = wikidataType.substring(wikidataType.lastIndexOf("/") + 1, wikidataType.length() - 1);

					if (wikidataToDBO != null) {
						if (!wikidataToDBO.containsKey(wikidataType))
							wikidataToDBO.put(wikidataType, new HashSet<String>());
						wikidataToDBO.get(wikidataType).add(type1);
					}
				}

				else if (parts[1].equals("<http://www.w3.org/2000/01/rdf-schema#subClassOf>")) {

					String type1 = parts[0];
					if (!type1.startsWith("<http://dbpedia.org/ontology"))
						continue;
					type1 = type1.substring(type1.lastIndexOf("/") + 1, type1.length() - 1);

					String type2 = parts[2];
					if (!type2.startsWith("<http://dbpedia.org/ontology"))
						continue;
					type2 = type2.substring(type2.lastIndexOf("/") + 1, type2.length() - 1);

					if (!parentClasses.containsKey(type1))
						parentClasses.put(type1, new HashSet<String>());
					parentClasses.get(type1).add(type2);

					if (datastoreWriter != null) {
						datastoreWriter.startInstance();
						datastoreWriter.writeDBPediaOntologySubClassTriple(type1, type2,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.DBPEDIA), true);
						datastoreWriter.endInstance();
					}
				} else if (parts[1].equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
						&& parts[2].equals("<http://www.w3.org/2002/07/owl#Class>")) {
					String type1 = parts[0];
					if (!type1.startsWith("<http://dbpedia.org/ontology"))
						continue;
					type1 = type1.substring(type1.lastIndexOf("/") + 1, type1.length() - 1);

					if (datastoreWriter != null) {
						datastoreWriter.startInstance();
						datastoreWriter.writeDBPediaOntologyTypeTriple(type1, prefixList.getPrefix(PrefixEnum.OWL),
								"Class", DataSets.getInstance().getDataSetWithoutLanguage(Source.DBPEDIA), true);
						datastoreWriter.endInstance();
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		createTransitiveMap(parentClasses);

		return parentClasses;
	}

	private static void createTransitiveMap(Map<String, Set<String>> parentClasses) {

		System.out.println("Create transitive type map.");

		boolean changed = true;
		while (changed) {
			changed = false;
			Map<String, Set<String>> changeSet = new HashMap<String, Set<String>>();
			for (String child : parentClasses.keySet()) {
				for (String parent : parentClasses.get(child)) {

					if (parentClasses.containsKey(parent)) {
						for (String parentParent : parentClasses.get(parent)) {
							if (parentClasses.get(child).contains(parentParent))
								continue;
							changed = true;
							if (!changeSet.containsKey(child))
								changeSet.put(child, new HashSet<String>());
							changeSet.get(child).add(parentParent);
						}
					}

				}
			}

			for (String child : changeSet.keySet()) {
				parentClasses.get(child).addAll(changeSet.get(child));
			}

		}

		// System.out.println("SportsEvent: " +
		// parentClasses.get("SportsEvent"));
		// System.out.println("TennisPlayer: " +
		// parentClasses.get("TennisPlayer"));
		// System.out.println("Fish: " + parentClasses.get("Fish"));
		// System.out.println("Species: " + parentClasses.get("Species"));
		// System.out.println("Taxon: " + parentClasses.get("Taxon"));
		// System.out.println("Disease: " + parentClasses.get("Disease"));

		System.out.println("\tDone.");

	}

	public Map<Entity, Set<String>> getTypesPerEntity() {
		return typesPerEntity;
	}

	public Map<String, Set<String>> getWikidataToDBO() {
		return wikidataToDBO;
	}

	public Map<String, Set<String>> getParentClasses() {
		return parentClasses;
	}

}
