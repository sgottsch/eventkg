package de.l3s.eventkg.wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.wikipedia.model.LinkSetCount;

public class WikipediaLinkSetsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	private Map<Language, Map<Event, Map<Entity, Integer>>> pairs;
	private Set<LinkSetCount> linkSets;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		WikipediaLinkSetsExtractor extr = new WikipediaLinkSetsExtractor(languages, allEventPagesDataSet);
		extr.run();
	}

	public WikipediaLinkSetsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("CurrentEventsRelationsExtraction", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract relations between entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect links co-occurrences.");
		extractRelations();
	}

	private void extractRelations() {

		this.pairs = new HashMap<Language, Map<Event, Map<Entity, Integer>>>();

		for (Language language : this.languages) {
			this.pairs.put(language, new HashMap<Event, Map<Entity, Integer>>());
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_SETS, language)) {
				processFile(child, language);
			}
		}

		this.linkSets = new HashSet<LinkSetCount>();
		for (Language language : this.pairs.keySet()) {
			for (Event event : this.pairs.get(language).keySet()) {
				for (Entity entity : this.pairs.get(language).get(event).keySet()) {
					linkSets.add(
							new LinkSetCount(event, entity, this.pairs.get(language).get(event).get(entity), language));
				}
			}
		}

		writeResults();
	}

	private void writeResults() {

		for (LinkSetCount linkCount : this.linkSets) {
			DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
		}

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

		Map<Event, Map<Entity, Integer>> pairs = this.pairs.get(language);

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);

				for (int i = 2; i < parts.length; i++) {
					String part = parts[i];

					Set<Entity> entities = new HashSet<Entity>();
					for (String entityName : part.split(" ")) {
						Entity entity = this.allEventPagesDataSet.getWikidataIdMappings()
								.getEntityByWikipediaLabel(language, entityName);
						if (entity != null)
							entities.add(entity);
					}

					Set<Entity> doneEntities = new HashSet<Entity>();

					for (Entity entity1 : entities) {
						doneEntities.add(entity1);
						for (Entity entity2 : entities) {

							// only take each pair once
							if (doneEntities.contains(entity2))
								continue;

							if (entity1.getEventEntity() != null) {
								if (!pairs.containsKey(entity1.getEventEntity()))
									pairs.put(entity1.getEventEntity(), new HashMap<Entity, Integer>());
								if (!pairs.get(entity1.getEventEntity()).containsKey(entity2)) {
									pairs.get(entity1.getEventEntity()).put(entity2, 1);
								} else {
									pairs.get(entity1.getEventEntity()).put(entity2,
											pairs.get(entity1.getEventEntity()).get(entity2) + 1);
								}
							}
							if (entity2.getEventEntity() != null) {
								if (!pairs.containsKey(entity2.getEventEntity()))
									pairs.put(entity2.getEventEntity(), new HashMap<Entity, Integer>());
								if (!pairs.get(entity2.getEventEntity()).containsKey(entity1)) {
									pairs.get(entity2.getEventEntity()).put(entity1, 1);
								} else {
									pairs.get(entity2.getEventEntity()).put(entity1,
											pairs.get(entity2.getEventEntity()).get(entity1) + 1);
								}
							}
						}
					}

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(pairs.keySet().size());

	}

}
