package de.l3s.eventkg.source.wikidata;

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
import gnu.trove.set.hash.THashSet;

public class WikidataTypesExtractor extends Extractor {

	private Map<Entity, Set<String>> typesPerEntity = new HashMap<Entity, Set<String>>();
	private Map<String, Set<String>> parentClasses = new HashMap<String, Set<String>>();

	private Set<Integer> usedTypes = new THashSet<Integer>();
	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter datastoreWriter;

	public WikidataTypesExtractor(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("DBpediaTypesExtractor", Source.WIKIDATA, "Loads all Wikidata types.", languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.datastoreWriter = dataStoreWriter;
	}

	@Override
	public void run() {

		PrefixList prefixList = PrefixList.getInstance();

		createTypeHierarchy();

		Set<String> usedLines = new HashSet<String>();
		BufferedReader br = null;
		DataSet dataSet = DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA);

		try {
			br = FileLoader.getReader(FileName.WIKIDATA_INSTANCE_OF);
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split("\t");

				String instanceId = parts[0];
				String type = parts[2];

				Entity eventKGEntity = this.wikidataIdMappings.getEntityByWikidataId(instanceId);
				if (eventKGEntity == null) {
					continue;
				}

				String eventKGId = eventKGEntity.getId();
				if (eventKGId == null) {
					continue;
				}

				String lineId = eventKGId + " " + type;

				if (usedLines.contains(lineId))
					continue;
				usedLines.add(lineId);

				if (!typesPerEntity.containsKey(eventKGEntity))
					typesPerEntity.put(eventKGEntity, new HashSet<String>());

				usedTypes.add(Integer.valueOf(type.substring(1)));
				typesPerEntity.get(eventKGEntity).add(type);

				datastoreWriter.startInstance();
				datastoreWriter.writeWikidataTypeTriple(eventKGEntity, prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY),
						type, dataSet, false);
				datastoreWriter.endInstance();
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public Set<String> getTransitiveClasses(Entity eventKGEntity) {
		Set<String> types = new HashSet<String>();
		if (typesPerEntity.containsKey(eventKGEntity))
			for (String parent : typesPerEntity.get(eventKGEntity)) {
				types.add(parent);
				if (parentClasses.containsKey(parent))
					for (String parentParent : parentClasses.get(parent)) {
						types.add(parentParent);
					}
			}
		// types.add(eventKGId);
		return types;
	}

	private void createTypeHierarchy() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.WIKIDATA_SUBCLASS_OF);
			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");
				String type1 = parts[0];
				String type2 = parts[2];

				if (!parentClasses.containsKey(type1))
					parentClasses.put(type1, new HashSet<String>());
				parentClasses.get(type1).add(type2);
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

		createTransitiveMap();
	}

	private void createTransitiveMap() {

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

		System.out.println("https://www.wikidata.org/wiki/Q847017: " + parentClasses.get("Q847017"));

		System.out.println("\tDone.");

	}

	public Map<Entity, Set<String>> getTypesPerEntity() {
		return typesPerEntity;
	}

	public Map<String, Set<String>> getParentClasses() {
		return parentClasses;
	}

	public Set<Integer> getUsedTypes() {
		return usedTypes;
	}

}
