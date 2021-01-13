package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
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

public class WikipediaLinkCountsExtractor extends Extractor {

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter triplesWriter;

	private Map<Entity, Set<Entity>> connectedEntities;

	public WikipediaLinkCountsExtractor(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter triplesWriter, Map<Entity, Set<Entity>> connectedEntities) {
		super("WikipediaLinkCountsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract Wikipedia link counts between entities and events.", languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.triplesWriter = triplesWriter;
		this.connectedEntities = connectedEntities;
	}

	public void run() {
		System.out.println("Collect links from and to event pages.");
		extractRelations();
	}

	private void extractRelations() {
		for (Language language : this.languages) {
			System.out.println(language);
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_COUNTS, language)) {
				processFileIterator(child, language);
			}
		}

		writeResults();
	}

	private void writeResults() {

		for (Entity entity : this.wikidataIdMappings.getEntities()) {
			if (entity.getLinkCounts() != null) {
				for (Entity targetEntity : entity.getLinkCounts().keySet()) {
					this.triplesWriter.startInstance();
					this.triplesWriter.writeLinkCount(entity, targetEntity, entity.getLinkCounts().get(targetEntity));
					this.triplesWriter.endInstance();
				}
				entity.clearLinkCounts();
			}
		}
	}

	private void processFileIterator(File file, Language language) {

		System.out.println(" " + file.getName());

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				processLine(line, language, file);
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

	private void processLine(String line, Language language, File file) {
		String[] parts = line.split(Config.TAB);
		String pageTitle = parts[1].replaceAll(" ", "_");
		Entity pageEntity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, pageTitle);

		if (pageEntity != null) {

			if (!pageEntity.isEvent() && !connectedEntities.containsKey(pageEntity))
				return;

			for (int i = 2; i < parts.length; i++) {
				String linkedPageTitle = parts[i].split(" ")[0].replaceAll(" ", "_");
				Entity linkedEntity = this.wikidataIdMappings.getEntityByWikipediaLabel(language, linkedPageTitle);
				int count = 0;
				if (linkedEntity != null) {

					try {
						count = Integer.valueOf(parts[i].split(" ")[1]);
					} catch (Exception e) {
						System.out.println("Warning: Error in file " + file.getName() + " for " + pageTitle + ".");
						continue;
					}

					if (pageEntity.isEvent() || linkedEntity.isEvent()) {
						pageEntity.addLinkCount(linkedEntity, language, count);
					} else if (areConnectedViaRelation(pageEntity, linkedEntity)) {
						pageEntity.addLinkCount(linkedEntity, language, count);
					}

				}

			}
		}

	}

	private boolean areConnectedViaRelation(Entity entity1, Entity entity2) {
		return connectedEntities.containsKey(entity1) && connectedEntities.get(entity1).contains(entity2);
	}

}
