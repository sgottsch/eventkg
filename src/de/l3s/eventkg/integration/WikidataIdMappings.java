package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.source.wikidata.WikidataResource;
import de.l3s.eventkg.source.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class WikidataIdMappings {

	// private Map<Language, Map<String, String>> wikidataEntitiesByIDs;
	// private Map<Language, Map<String, String>> wikidataIdsForWikipediaLabels;
	private Map<Language, Map<String, String>> wikidataPropertysByIDs;

	private TIntObjectMap<Entity> entitiesByWikidataNumericIds = new TIntObjectHashMap<Entity>(45000000);

	// private Map<String, Entity> entitiesByWikidataIds;
	private Map<Language, Map<String, Entity>> entitiesByWikipediaLabels;

	private List<Language> languages;

	// private Set<String> wikidataIdsThatHaveLabels = new HashSet<String>();

	private Map<String, TimeSymbol> temporalPropertyIds;

	private Map<Language, Map<String, String>> redirects = new HashMap<Language, Map<String, String>>();

	public WikidataIdMappings(List<Language> languages) {
		this.languages = languages;
	}

	public void load() {
		loadRedirects();
		loadWikidataIdMapping();
		loadWikidataLabels();
		loadWikidataPropertyIdMapping();
		loadTemporalProperties();
	}

	private void loadRedirects() {
		for (Language language : this.languages) {
			System.out.println(language + ": Load redirects.");
			Map<String, String> redirectsOfLanguage = RedirectsTableCreator.getRedirects(language);
			this.redirects.put(language, redirectsOfLanguage);
		}
	}

	private void loadWikidataLabels() {

		Set<Integer> allWikidataEntitiesWithFacts = loadAllWikidataEntitiesWithFacts();

		int ignoredEntities = 0;

		for (Language language : this.languages) {

			System.out
					.println("Load Wikidata mapping for the " + language.getLanguageAdjective() + " Wikidata labels.");

			int lineNo = 0;
			int brackets = 0;
			BufferedReader br = null;
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_LABELS, language);

				String line;
				while ((line = br.readLine()) != null) {

					// ignore all entities that do not have a Wikipedia label
					// and no temporal relation

					if (lineNo % 1000000 == 0)
						System.out.println(lineNo + " - " + brackets);

					lineNo += 1;
					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String label = parts[1].trim();

					if (label.startsWith(WikiWords.getInstance().getCategoryLabel(language) + ":"))
						continue;

					if (label.isEmpty())
						continue;

					// if(label.startsWith("[") && label.endsWith("]")) {
					// brackets+=1;
					// continue;
					// }

					int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

					// Entity entity =
					// this.entitiesByWikidataIds.get(wikidataId);

					Entity entity = this.entitiesByWikidataNumericIds.get(numericWikidataId);
					if (entity == null) {
						// all entities that do not have Wikipedia labels

						if (!allWikidataEntitiesWithFacts.contains(numericWikidataId)) {
							// System.out.println("Ignore " + numericWikidataId
							// + " - " + label + ".");
							ignoredEntities += 1;
							continue;
						}

						entity = new Entity(wikidataId);
						DataStore.getInstance().addEntity(entity);
						// this.entitiesByWikidataIds.put(wikidataId, entity);
						this.entitiesByWikidataNumericIds.put(numericWikidataId, entity);
					}

					entity.addWikidataLabel(language, label);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println(language + ", ignoredEntities: " + ignoredEntities);
			System.out.println(language + ", entities: " + this.entitiesByWikidataNumericIds.size());

		}

		System.out.println("ignoredEntities: " + ignoredEntities);
		System.out.println(this.entitiesByWikidataNumericIds.size() + " entities.");

	}

	private Set<Integer> loadAllWikidataEntitiesWithFacts() {

		System.out.println("loadAllWikidataEntitiesWithFacts");

		Set<Integer> wikidataIds = new HashSet<Integer>();

		BufferedReader br = null;

		try {

			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_FACTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t"))
					continue;

				wikidataIds.add(Integer.valueOf(parts[1].substring(1)));
				wikidataIds.add(Integer.valueOf(parts[3].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				wikidataIds.add(Integer.valueOf(parts[2].substring(1)));
				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				// only add parent: We are only interested in subs if they are
				// actually the location of something. Parents are needed for
				// transitivity.
				wikidataIds.add(Integer.valueOf(parts[1].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(Config.TAB);
				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\t" + wikidataIds.size() + " (with scientific articles)");

		// remove scientific articles
		// TODO: Do this as blacklist class

		br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_INSTANCE_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				String parentClass = parts[2];

				// remove scientific article and Wikinews articles
				if (parentClass.equals(WikidataResource.SCIENTIFIC_ARTICLE.getId())
						|| parentClass.equals(WikidataResource.WIKINEWS_ARTICLE.getId())) {
					wikidataIds.remove(Integer.valueOf(parts[0].substring(1)));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\t" + wikidataIds.size());

		return wikidataIds;
	}

	private void loadWikidataIdMapping() {

		// wikipediaLabelsForWikidataIds = new HashMap<String, String>();
		// this.wikidataIdsForWikipediaLabels = new HashMap<Language,
		// Map<String, String>>();
		// this.wikidataEntitiesByIDs = new HashMap<Language, Map<String,
		// String>>();

		// this.entitiesByWikidataIds = new HashMap<String, Entity>();
		this.entitiesByWikipediaLabels = new HashMap<Language, Map<String, Entity>>();

		for (Language language : this.languages) {

			System.out.println("Load Wikidata mapping for the " + language.getLanguageAdjective()
					+ " Wikipedia labels in Wikidata.");

			this.entitiesByWikipediaLabels.put(language, new HashMap<String, Entity>());
			int lines = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.ID_TO_WIKIPEDIA_MAPPING_FILE_NAME, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String wikipediaLabel = parts[1].replaceAll(" ", "_");

					// wikipediaLabelsForWikidataIds.put(wikidataId,
					// wikipediaLabel);
					// wikidataIdsForWikipediaLabels.get(language).put(wikipediaLabel,
					// wikidataId);
					// wikidataEntitiesByIDs.get(language).put(wikidataId,
					// wikipediaLabel);

					lines += 1;
					createEntity(wikidataId, language, wikipediaLabel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println(language + ": " + lines + " labels.");
		}

		Set<Entity> entitiesToRemove = new HashSet<Entity>();
		// remove category entities
		for (Entity entity : this.entitiesByWikidataNumericIds.valueCollection()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getCategoryPrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						entitiesToRemove.add(entity);
					}
				}
			}
		}

		// remove list entities
		for (Entity entity : this.entitiesByWikidataNumericIds.valueCollection()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getListPrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						entitiesToRemove.add(entity);
					}
				}
			}
		}

		for (Entity entity : entitiesToRemove) {
			this.entitiesByWikidataNumericIds.remove(entity.getNumericWikidataId());
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				this.entitiesByWikipediaLabels.get(labelLanguage).remove(entity.getWikipediaLabel(labelLanguage));
				DataStore.getInstance().removeEntity(entity);
			}
		}

	}

	private Entity createEntity(String wikidataId, Language language, String wikipediaLabel) {

		int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

		Entity entity = this.entitiesByWikidataNumericIds.get(numericWikidataId);
		if (entity != null) {
			entity.addWikipediaLabel(language, wikipediaLabel);
		} else {
			entity = new Entity(language, wikipediaLabel, wikidataId, numericWikidataId);
			DataStore.getInstance().addEntity(entity);
			this.entitiesByWikidataNumericIds.put(numericWikidataId, entity);
		}

		this.entitiesByWikipediaLabels.get(language).put(wikipediaLabel, entity);

		return entity;
	}

	private void loadWikidataPropertyIdMapping() {

		// wikipediaLabelsForWikidataIds = new HashMap<String, String>();
		wikidataPropertysByIDs = new HashMap<Language, Map<String, String>>();

		for (Language language : this.languages) {

			wikidataPropertysByIDs.put(language, new HashMap<String, String>());

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.WIKIDATA_LABELS_PROPERTIES, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String wikipediaLabel = parts[1];

					wikidataPropertysByIDs.get(language).put(wikidataId, wikipediaLabel);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public String getWikidataPropertysByID(Language language, String propertyId) {
		return this.wikidataPropertysByIDs.get(language).get(propertyId);
	}

	public TimeSymbol getWikidataTemporalPropertyTypeById(String propertyId) {
		return this.temporalPropertyIds.get(propertyId);
	}

	public void loadTemporalProperties() {
		this.temporalPropertyIds = new HashMap<String, TimeSymbol>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");
				String id = parts[0];

				TimeSymbol propType = TimeSymbol.fromString(parts[2]);

				this.temporalPropertyIds.put(id, propType);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Entity getEntityByWikipediaLabel(Language language, String wikipediaLabel) {
		if (!this.redirects.containsKey(language))
			System.out.println("redirects is missing " + language);

		if (!this.entitiesByWikipediaLabels.containsKey(language))
			System.out.println("entitiesByWikipediaLabels is missing " + language);

		if (this.redirects.get(language).containsKey(wikipediaLabel))
			wikipediaLabel = this.redirects.get(language).get(wikipediaLabel);

		return entitiesByWikipediaLabels.get(language).get(wikipediaLabel);
	}

	public Entity getEntityByWikidataId(String wikidataId) {
		return entitiesByWikidataNumericIds.get(Integer.parseInt(wikidataId.substring(1)));
	}

	public Entity getEntityByWikidataId(int wikidataId) {
		return entitiesByWikidataNumericIds.get(wikidataId);
	}

	public TIntObjectMap<Entity> getEntitiesByWikidataIds() {
		return this.entitiesByWikidataNumericIds;
	}

	public TIntObjectMap<Entity> getEntitiesByWikidataNumericIds() {
		return entitiesByWikidataNumericIds;
	}

	public void updateEntityToEvent(Entity entity, Event event) {
		this.entitiesByWikidataNumericIds.put(entity.getNumericWikidataId(), event);

		for (Language language : entity.getWikipediaLabels().keySet()) {
			entitiesByWikipediaLabels.get(language).put(entity.getWikipediaLabels().get(language), event);
		}
	}

	// public Set<String> getWikidataIdsThatHaveLabels() {
	// return wikidataIdsThatHaveLabels;
	// }

}
