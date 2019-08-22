package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikipedia.model.LinkSetCount;
import de.l3s.eventkg.textual_events.TextualEventsExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class WikipediaLinkSetsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	private Map<Language, Map<Entity, Map<Entity, Integer>>> pairs;
	private Set<LinkSetCount> linkSets;

	public WikipediaLinkSetsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("WikipediaLinkSetsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract the number of co-occurences of entities and events in the same sentences in Wikipedia.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect links co-occurrences.");
		extractRelations();
	}

	private void extractRelations() {

		this.pairs = new THashMap<Language, Map<Entity, Map<Entity, Integer>>>();

		for (Language language : this.languages) {
			this.pairs.put(language, new THashMap<Entity, Map<Entity, Integer>>());
			loadCountsFromTextualEvents(language);
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_SETS, language)) {
				processFile(child, language);
			}
		}

		this.linkSets = new THashSet<LinkSetCount>();
		for (Iterator<Language> it = this.pairs.keySet().iterator(); it.hasNext();) {
			Language language = it.next();
			for (Entity entity1 : this.pairs.get(language).keySet()) {
				for (Entity entity2 : this.pairs.get(language).get(entity1).keySet()) {
					linkSets.add(new LinkSetCount(entity1, entity2, this.pairs.get(language).get(entity1).get(entity2),
							language));
				}
			}
			it.remove();
		}

		this.pairs = null;

		writeResults();
	}

	private void loadCountsFromTextualEvents(Language language) {

		if (!TextualEventsExtractor.PUT_ENTITIES_LINKED_IN_THE_SAME_EVENT_IN_LINK_SET)
			return;

		System.out.println("loadCountsFromTextualEvents " + language);

		int testCount = 0;

		Map<Entity, Map<Entity, Integer>> pairs = this.pairs.get(language);

		Set<Entity> doneEntities = new THashSet<Entity>();
		for (Entity entity1 : DataStore.getInstance().getMentionCountsFromTextualEvents().get(language).keySet()) {
			doneEntities.add(entity1);
			for (Entity entity2 : DataStore.getInstance().getMentionCountsFromTextualEvents().get(language).keySet()) {

				// only take each pair once
				if (doneEntities.contains(entity2))
					continue;

				if (testCount < 10) {
					System.out.println("\t" + entity1.getWikidataId() + "\t" + entity2.getWikidataId());
				}

				if (entity1.isEvent()) {
					if (!pairs.containsKey(entity1))
						pairs.put(entity1, new THashMap<Entity, Integer>());
					if (!pairs.get(entity1).containsKey(entity2)) {
						pairs.get(entity1).put(entity2, 1);
					} else {
						pairs.get(entity1).put(entity2, pairs.get(entity1).get(entity2) + 1);
					}
				} else if (entity2.isEvent()) {
					if (!pairs.containsKey(entity2))
						pairs.put(entity2, new THashMap<Entity, Integer>());
					if (!pairs.get(entity2).containsKey(entity1)) {
						pairs.get(entity2).put(entity1, 1);
					} else {
						pairs.get(entity2).put(entity1, pairs.get(entity2).get(entity1) + 1);
					}
				} else if (areConnectedViaRelation(entity1, entity2)) {
					if (testCount < 10) {
						System.out.println("\t\tareConnectedViaRelation");
					}

					if (!pairs.containsKey(entity1))
						pairs.put(entity1, new THashMap<Entity, Integer>());
					if (!pairs.get(entity1).containsKey(entity2)) {
						pairs.get(entity1).put(entity2, 1);
					} else {
						pairs.get(entity1).put(entity2, pairs.get(entity1).get(entity2) + 1);
					}
				}
			}

			testCount += 1;
		}

		// not needed anymore, free space
		DataStore.getInstance().getMentionCountsFromTextualEvents().get(language).clear();
	}

	private void writeResults() {

		int i = 0;
		int size = this.linkSets.size();

		System.out.println("Found " + size + " relations.");
		for (Iterator<LinkSetCount> it = this.linkSets.iterator(); it.hasNext();) {
			LinkSetCount linkCount = it.next();
			if (i % 100000 == 0)
				System.out.println("\t" + i + "/" + size + " (" + ((double) i / size) + ")");
			i += 1;
			DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
			DataStore.getInstance().addLinkRelation(linkCount.toGenericRelationSubjectObjectReverted());
			it.remove();
		}
		System.out.println(" Finished loading link sets.");

		this.linkSets = null;

		// System.out.println("Write results: Link sets");
		// PrintWriter writer = null;
		// try {
		// writer = FileLoader.getWriter(FileName.ALL_LINK_SETS);
		//
		// for (LinkSetCount linkCount : this.linkSets) {
		//
		// DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
		//
		// writer.write(linkCount.getEvent().getWikidataId());
		// writer.write(Config.TAB);
		// writer.write(linkCount.getEvent().getWikipediaLabelsString(this.languages));
		// writer.write(Config.TAB);
		// writer.write(linkCount.getEntity().getWikidataId());
		// writer.write(Config.TAB);
		// writer.write(linkCount.getEntity().getWikipediaLabelsString(this.languages));
		// writer.write(Config.TAB);
		// writer.write(String.valueOf(linkCount.getCount()));
		// writer.write(Config.TAB);
		// writer.write(linkCount.getLanguage().getLanguageLowerCase());
		// writer.write(Config.NL);
		// }
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } finally {
		// writer.close();
		// }

	}

	private void processFile(File file, Language language) {

		Map<Entity, Map<Entity, Integer>> pairs = this.pairs.get(language);

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);

				for (int i = 2; i < parts.length; i++) {
					String part = parts[i];

					Set<Entity> entities = new THashSet<Entity>();
					for (String entityName : part.split(" ")) {
						Entity entity = this.allEventPagesDataSet.getWikidataIdMappings()
								.getEntityByWikipediaLabel(language, entityName);
						if (entity != null)
							entities.add(entity);
					}

					Set<Entity> doneEntities = new THashSet<Entity>();
					for (Entity entity1 : entities) {
						doneEntities.add(entity1);
						for (Entity entity2 : entities) {

							// only take each pair once
							if (doneEntities.contains(entity2))
								continue;

							if (entity1.isEvent()) {
								if (!pairs.containsKey(entity1))
									pairs.put(entity1, new THashMap<Entity, Integer>());
								if (!pairs.get(entity1).containsKey(entity2)) {
									pairs.get(entity1).put(entity2, 1);
								} else {
									pairs.get(entity1).put(entity2, pairs.get(entity1).get(entity2) + 1);
								}
							} else if (entity2.isEvent()) {
								if (!pairs.containsKey(entity2))
									pairs.put(entity2, new THashMap<Entity, Integer>());
								if (!pairs.get(entity2).containsKey(entity1)) {
									pairs.get(entity2).put(entity1, 1);
								} else {
									pairs.get(entity2).put(entity1, pairs.get(entity2).get(entity1) + 1);
								}
							} else if (areConnectedViaRelation(entity1, entity2)) {
								if (!pairs.containsKey(entity1))
									pairs.put(entity1, new THashMap<Entity, Integer>());
								if (!pairs.get(entity1).containsKey(entity2)) {
									pairs.get(entity1).put(entity2, 1);
								} else {
									pairs.get(entity1).put(entity2, pairs.get(entity1).get(entity2) + 1);
								}
							}

						}
					}

				}

			}

		} catch (

		IOException e) {
			e.printStackTrace();
		}

		System.out.println(pairs.keySet().size() + "\t" + file.getName());
	}

	private boolean areConnectedViaRelation(Entity entity1, Entity entity2) {
		return DataStore.getInstance().getConnectedEntities().containsKey(entity1)
				&& DataStore.getInstance().getConnectedEntities().get(entity1).contains(entity2);
	}

}
