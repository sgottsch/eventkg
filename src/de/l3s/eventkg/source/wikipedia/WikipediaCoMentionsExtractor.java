package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.set.hash.THashSet;

public class WikipediaCoMentionsExtractor extends Extractor {

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter triplesWriter;

	private Map<Entity, Set<Entity>> connectedEntities;

	public WikipediaCoMentionsExtractor(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter triplesWriter, Map<Entity, Set<Entity>> connectedEntities) {
		super("WikipediaLinkSetsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract the number of co-occurences of entities and events in the same sentences in Wikipedia.",
				languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.triplesWriter = triplesWriter;
		this.connectedEntities = connectedEntities;
	}

	public void run() {
		System.out.println("Collect links co-occurrences.");
		extractMentions();
	}

	private void extractMentions() {

		for (Language language : this.languages) {
			System.out.println("Extract mentions: " + language);
			loadCountsFromTextualEvents(language);
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_SETS, language)) {
				processFile(child, language);
			}
		}

		writeResults();
	}

	private void loadCountsFromTextualEvents(Language language) {

		System.out.println(" Extract mentions from textual events.");

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.TEXT_EVENT_LINKS, language);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(",");

				Set<Entity> entities = new HashSet<Entity>();

				for (String wikidataID : parts) {
					Entity entity = wikidataIdMappings.getEntityByWikidataId(wikidataID);
					if (entity != null)
						entities.add(entity);
				}

				addMentions(language, entities);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void writeResults() {

		for (Entity entity : this.wikidataIdMappings.getEntities()) {
			if (entity.getLinkCounts() != null) {
				for (Entity targetEntity : entity.getLinkCounts().keySet()) {
					this.triplesWriter.startInstance();
					this.triplesWriter.writeCoMentionCount(entity, targetEntity,
							entity.getLinkCounts().get(targetEntity));
					this.triplesWriter.endInstance();
				}
				entity.clearLinkCounts();
			}
		}

		System.out.println(" Finished writing co-mentions.");
	}

	private void processFile(File file, Language language) {

		System.out.println(" Extract mentions from " + file.getName() + ".");

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);

				for (int i = 2; i < parts.length; i++) {
					String part = parts[i];

					Set<Entity> entities = new THashSet<Entity>();
					for (String entityName : part.split(" ")) {
						Entity entity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, entityName);
						if (entity != null)
							entities.add(entity);
					}

					addMentions(language, entities);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void addMentions(Language language, Set<Entity> entities) {
		for (Entity entity1 : entities) {
			for (Entity entity2 : entities) {

				if (entity1 == entity2)
					continue;

				if (entity1.isEvent() || entity2.isEvent()) {
					entity1.increaseLinkCount(entity2, language);
				} else if (areConnectedViaRelation(entity1, entity2)) {
					entity1.increaseLinkCount(entity2, language);
				}

			}
		}
	}

	private boolean areConnectedViaRelation(Entity entity1, Entity entity2) {
		return connectedEntities.containsKey(entity1) && connectedEntities.get(entity1).contains(entity2);
	}

}
