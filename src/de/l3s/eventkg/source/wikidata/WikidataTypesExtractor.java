package de.l3s.eventkg.source.wikidata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriter;
import de.l3s.eventkg.integration.DataStoreWriterMode;
import de.l3s.eventkg.integration.EntityIdGenerator;
import de.l3s.eventkg.integration.model.FileType;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class WikidataTypesExtractor extends Extractor {

	private EntityIdGenerator eventKGIdMapping;

	private Map<String, Set<String>> typesPerEntity = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> parentClasses = new HashMap<String, Set<String>>();

	public WikidataTypesExtractor(List<Language> languages, EntityIdGenerator eventKGIdMapping) {
		super("DBpediaTypesExtractor", Source.WIKIDATA, "Loads all Wikidata types.", languages);
		this.eventKGIdMapping = eventKGIdMapping;
	}

	@Override
	public void run() {

		PrefixList prefixList = PrefixList.getInstance();

		FileType fileType = FileType.NQ;

		PrintWriter writer = null;
		PrintWriter writerPreview = null;

		DataStoreWriter dataStoreWriter = new DataStoreWriter(languages,
				DataStoreWriterMode.USE_IDS_OF_CURRENT_EVENTKG_VERSION);
		dataStoreWriter.initPrefixes();

		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_TYPES_WIKIDATA);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_TYPES_WIKIDATA_PREVIEW);

			List<Prefix> prefixes = new ArrayList<Prefix>();
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDF));
			prefixes.add(prefixList.getPrefix(PrefixEnum.RDFS));
			prefixes.add(prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY));
			for (String line : dataStoreWriter.createIntro(prefixes, prefixList, fileType)) {
				writer.write(line + Config.NL);
				writerPreview.write(line + Config.NL);
			}

			createTypeHierarchy();

			Set<String> usedLines = new HashSet<String>();
			int lineNo = 0;
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

					String eventKGId = eventKGIdMapping.getEventIDByWikidataId(instanceId);
					if (eventKGId == null) {
						continue;
					}
					lineNo += 1;

					String lineId = eventKGId + " " + type;

					if (usedLines.contains(lineId))
						continue;
					usedLines.add(lineId);

					if (!typesPerEntity.containsKey(eventKGId))
						typesPerEntity.put(eventKGId, new HashSet<String>());

					typesPerEntity.get(eventKGId).add(type);

					dataStoreWriter.writeTriple(writer, writerPreview, lineNo, dataStoreWriter.getBasePrefix(),
							eventKGId, prefixList.getPrefix(PrefixEnum.RDF), "type",
							prefixList.getPrefix(PrefixEnum.WIKIDATA_ENTITY), type, false, dataSet, fileType);

				}
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} finally {
				br.close();
			}

		} catch (

		IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

		System.out.println("event_437985: " + typesPerEntity.get("event_437985"));
		System.out.println("<event_437985>: " + typesPerEntity.get("event_437985"));
		System.out.println("<event_437985> transitive: " + getTransitiveClasses("<event_437985>"));

		System.out.println("entity_4956997: " + typesPerEntity.get("entity_4956997"));
		System.out.println("<entity_4956997>: " + typesPerEntity.get("<entity_4956997>"));
		System.out.println("<entity_4956997> transitive: " + getTransitiveClasses("<entity_4956997>"));

	}

	public Set<String> getTransitiveClasses(String eventKGId) {
		Set<String> types = new HashSet<String>();
		if (typesPerEntity.containsKey(eventKGId))
			for (String parent : typesPerEntity.get(eventKGId)) {
				types.add(parent);
				if (parentClasses.containsKey(parent))
					for (String parentParent : parentClasses.get(parent)) {
						types.add(parentParent);
					}
			}
		types.add(eventKGId);
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

	public Map<String, Set<String>> getTypesPerEntity() {
		return typesPerEntity;
	}

	public Map<String, Set<String>> getParentClasses() {
		return parentClasses;
	}

}
