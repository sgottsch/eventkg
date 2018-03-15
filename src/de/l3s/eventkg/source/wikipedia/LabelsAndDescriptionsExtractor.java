package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.Label;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LabelsAndDescriptionsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	private Map<Language, Map<Event, String>> sentences;

	private HashMap<Language, Map<Entity, String>> wikipediaLabels;
	private HashMap<Language, Map<Entity, Set<String>>> wikidataLabels;

	private HashMap<Language, Map<Entity, Set<String>>> aliases;

	private HashMap<Language, Map<Event, String>> descriptions;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		LabelsAndDescriptionsExtractor extr = new LabelsAndDescriptionsExtractor(languages, allEventPagesDataSet);
		extr.run();
	}

	public LabelsAndDescriptionsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("CurrentEventsRelationsExtraction", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect labels and descriptions of entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		extractRelations();
	}

	private void extractRelations() {

		this.sentences = new HashMap<Language, Map<Event, String>>();
		for (Language language : this.languages) {
			sentences.put(language, new HashMap<Event, String>());
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_FIRST_SENTENCES, language)) {
				processFile(child, language);
			}
		}

		this.wikipediaLabels = new HashMap<Language, Map<Entity, String>>();
		for (Language language : this.languages) {
			this.wikipediaLabels.put(language, new HashMap<Entity, String>());
		}

		this.wikidataLabels = new HashMap<Language, Map<Entity, Set<String>>>();
		for (Language language : this.languages) {
			this.wikidataLabels.put(language, new HashMap<Entity, Set<String>>());
		}

		collectLabels();

		this.aliases = new HashMap<Language, Map<Entity, Set<String>>>();
		for (Language language : this.languages) {
			this.aliases.put(language, new HashMap<Entity, Set<String>>());
			collectAliasesFromFile(FileLoader.getFile(FileName.WIKIDATA_ALIASES, language), language);
		}

		this.descriptions = new HashMap<Language, Map<Event, String>>();
		for (Language language : this.languages) {
			this.descriptions.put(language, new HashMap<Event, String>());
			collectDescriptionsFromFile(FileLoader.getFile(FileName.WIKIDATA_DESCRIPTIONS, language), language);
		}

		writeResults();
	}

	private void collectDescriptionsFromFile(File file, Language language) {

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);
				String wikidataId = parts[0];

				Event event = allEventPagesDataSet.getEventByWikidataId(wikidataId);

				if (event != null) {
					this.descriptions.get(language).put(event, parts[1]);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("descriptions (" + language + "): " + this.descriptions.get(language).size());
	}

	private void collectAliasesFromFile(File file, Language language) {
		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);
				String wikidataId = parts[0];

				Event event = allEventPagesDataSet.getEventByWikidataId(wikidataId);

				if (event != null) {
					this.aliases.get(language).put(event, new HashSet<String>());

					boolean first = true;
					for (String alias : parts) {
						if (first) {
							first = false;
							continue;
						}
						this.aliases.get(language).get(event).add(alias);
					}
				} else {
					Entity entity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(wikidataId);
					if (entity == null)
						continue;

					this.aliases.get(language).put(entity, new HashSet<String>());

					boolean first = true;
					for (String alias : parts) {
						if (first) {
							first = false;
							continue;
						}
						this.aliases.get(language).get(entity).add(alias);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("aliases (" + language + "): " + this.aliases.get(language).size());
	}

	private void collectLabels() {

		// for (Event event : this.allEventPagesDataSet.getEvents()) {
		// for (Language language : event.getWikipediaLabels().keySet()) {
		// this.labels.get(language).put(event,
		// event.getWikipediaLabel(language));
		// }
		// }

		for (Entity entity : this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataIds()
				.valueCollection()) {
			for (Language language : entity.getWikipediaLabels().keySet()) {
				this.wikipediaLabels.get(language).put(entity, entity.getWikipediaLabel(language));
			}

			for (Language language : entity.getWikidataLabels().keySet()) {
				this.wikidataLabels.get(language).put(entity, entity.getWikidataLabels().get(language));
			}
		}

	}

	private void writeResults() {

		System.out.println("Write results: labels, descriptions, aliases,...");
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_FIRST_SENTENCES);

			for (Language language : this.languages) {
				for (Event event : this.sentences.get(language).keySet()) {

					DataStore.getInstance()
							.addDescription(new Description(event,
									DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA),
									this.sentences.get(language).get(event), language));

					writer.write(event.getWikidataId());
					writer.write(Config.TAB);
					writer.write("wiki_first_sentence");
					writer.write(Config.TAB);
					writer.write(event.getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(this.sentences.get(language).get(event));
					writer.write(Config.NL);
				}
			}

			for (Language language : this.languages) {
				for (Event event : this.descriptions.get(language).keySet()) {

					DataStore.getInstance()
							.addDescription(new Description(event,
									DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA),
									this.descriptions.get(language).get(event), language));

					writer.write(event.getWikidataId());
					writer.write(Config.TAB);
					writer.write("wikidata_description");
					writer.write(Config.TAB);
					writer.write(event.getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(this.descriptions.get(language).get(event));
					writer.write(Config.NL);
				}
			}

			for (Language language : this.languages) {
				for (Entity entity : this.wikipediaLabels.get(language).keySet()) {

					DataStore.getInstance().addWikipediaLabel(
							new Label(entity, DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA),
									this.wikipediaLabels.get(language).get(entity), language));

					writer.write(entity.getWikidataId());
					writer.write(Config.TAB);
					writer.write("wiki_label");
					writer.write(Config.TAB);
					writer.write(entity.getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(this.wikipediaLabels.get(language).get(entity));
					writer.write(Config.NL);
				}

				for (Entity entity : this.wikidataLabels.get(language).keySet()) {

					for (String label : this.wikidataLabels.get(language).get(entity)) {
						DataStore.getInstance().addWikidataLabel(new Label(entity,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), label, language));

						writer.write(entity.getWikidataId());
						writer.write(Config.TAB);
						writer.write("wikidata_label");
						writer.write(Config.TAB);
						writer.write(label);
						writer.write(Config.NL);
					}
				}
			}

			for (Language language : this.languages) {
				for (Entity event : this.aliases.get(language).keySet()) {
					for (String alias : this.aliases.get(language).get(event)) {
						DataStore.getInstance().addAlias(new Alias(event,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), alias, language));

						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write("wikidata_alias");
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(alias);
						writer.write(Config.NL);
					}

				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void processFile(File file, Language language) {

		System.out.println("Process file " + file.getName() + ".");

		try {
			String content = FileLoader.readFile(file);

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);
				String pageTitle = parts[1].replaceAll(" ", "_");
				Entity pageEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
						pageTitle);

				if (pageEntity != null && pageEntity.isEvent() && !parts[2].isEmpty()) {
					this.sentences.get(language).put((Event) pageEntity, parts[2]);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
