package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.source.wikidata.WikidataResource;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.MemoryStatsUtil;
import de.l3s.eventkg.util.TimeTransformer;

public class WikidataIdMappingsOld {

	// private Map<Language, Map<String, String>> wikidataEntitiesByIDs;
	// private Map<Language, Map<String, String>> wikidataIdsForWikipediaLabels;
	private Map<Language, Map<String, String>> wikidataPropertysByIDs;

	private CacheManager cacheManager;

	private Cache<Integer, Entity> entitiesByWikidataNumericIds;

	private DataStore dataStore;

	// private Map<String, Entity> entitiesByWikidataIds;
	private Map<Language, Cache<String, Integer>> entitiesByWikipediaLabels;

	private List<Language> languages;

	private Map<String, TimeSymbol> temporalPropertyIds;

	// private Map<Language, Map<String, String>> redirects = new
	// HashMap<Language, Map<String, String>>();

	private Set<String> wikipediaInternalClasses;

	public WikidataIdMappingsOld(List<Language> languages) {
		this.languages = languages;

		// this.cacheManager =
		// CacheManagerBuilder.newCacheManagerBuilder().build(true);

		this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.with(CacheManagerBuilder.persistence(new File(Config.getValue("cache_folder")))).build(true);
	}

	public void load(boolean loadWikidataLabels) {

		// this.entitiesByWikidataNumericIds =
		// cacheManager.createCache("entitiesByWikidataNumericIds",
		// CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class,
		// Entity.class,
		// ResourcePoolsBuilder.heap(10000).offheap(1, MemoryUnit.GB)).build());

		this.entitiesByWikidataNumericIds = this.cacheManager.createCache("entitiesByWikidataNumericIds",
				CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, Entity.class,
						ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100000, EntryUnit.ENTRIES)
								.offheap(2, MemoryUnit.GB).disk(20, MemoryUnit.GB, false)));

		this.dataStore = DataStore.getInstance();

		loadWikipediaInternalClasses();
		// loadRedirects();
		loadWikidataIdMapping();
		if (loadWikidataLabels)
			loadWikidataLabels();

		System.out.println("Add entities to data store - " + TimeTransformer.getTime() + ".");
		for (Iterator<Entry<Integer, Entity>> it = this.entitiesByWikidataNumericIds.iterator(); it.hasNext();) {
			Entry<Integer, Entity> entry = it.next();
			dataStore.addEntity(entry.getValue());
		}
		System.out.println(" -> Done  - " + TimeTransformer.getTime() + ".");

		loadWikidataPropertyIdMapping();
		loadTemporalProperties();
	}

	private void loadWikipediaInternalClasses() {
		// TODO Auto-generated method stub

		this.wikipediaInternalClasses = new HashSet<String>();

		try {
			JSONObject wikidataJSON = new JSONObject(FileLoader.readFile(FileName.WIKIDATA_WIKIPEDIA_INTERNAL_ITEMS));
			JSONArray bindings = wikidataJSON.getJSONObject("results").getJSONArray("bindings");
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject binding = bindings.getJSONObject(i);

				JSONObject itemJSON = binding.getJSONObject("item");
				String item = itemJSON.getString("value");

				item = item.substring(item.lastIndexOf("/") + 1);

				this.wikipediaInternalClasses.add(item);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// private void loadRedirects() {
	// for (Language language : this.languages) {
	// System.out.println(language + ": Load redirects.");
	// Map<String, String> redirectsOfLanguage =
	// RedirectsTableCreator.getRedirects(language);
	// this.redirects.put(language, redirectsOfLanguage);
	// }
	// }

	private void loadWikidataLabels() {

		Set<Integer> allWikidataEntitiesWithFacts = loadAllWikidataEntitiesWithFacts();

		int ignoredEntities = 0;

		for (Language language : this.languages) {

			System.out.println("Load Wikidata label mapping for the " + language.getLanguageAdjective()
					+ " Wikidata labels - " + TimeTransformer.getTime() + ".");

			int lineNo = 0;

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.WIKIDATA_LABELS, language);
				while (it.hasNext()) {
					String line = it.nextLine();
					if (lineNo % 1000000 == 0)
						System.out.println(" Line " + lineNo + " - " + TimeTransformer.getTime() + ".");

					lineNo += 1;
					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String label = parts[1].trim();

					if (label.startsWith(WikiWords.getInstance().getCategoryLabel(language) + ":"))
						continue;

					if (label.startsWith(WikiWords.getInstance().getTemplateLabel(language) + ":"))
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
						// this.dataStore.addEntity(entity);
						// this.entitiesByWikidataIds.put(wikidataId, entity);
						this.entitiesByWikidataNumericIds.put(numericWikidataId, entity);
					}

					entity.addWikidataLabel(language, label);
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

			System.out.println(language + ", ignoredEntities: " + ignoredEntities);
			// System.out.println(language + ", entities: " +
			// this.entitiesByWikidataNumericIds.size());

			MemoryStatsUtil.printMemoryStats();

		}

		System.out.println("ignoredEntities: " + ignoredEntities);
		// System.out.println(this.entitiesByWikidataNumericIds. + "
		// entities.");

	}

	private Set<Integer> loadAllWikidataEntitiesWithFacts() {

		System.out.println("loadAllWikidataEntitiesWithFacts");

		Set<Integer> wikidataIds = new HashSet<Integer>();

		BufferedReader br1 = null;

		try {

			try {
				br1 = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_FACTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br1.readLine()) != null) {

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
				br1.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br2 = null;
		try {
			try {
				br2 = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br2.readLine()) != null) {
				String[] parts = line.split("\t");

				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br3 = null;
		try {
			try {
				br3 = FileLoader.getReader(FileName.WIKIDATA_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br3.readLine()) != null) {
				String[] parts = line.split("\t");

				try {
					wikidataIds.add(Integer.valueOf(parts[2].substring(1)));
					wikidataIds.add(Integer.valueOf(parts[0].substring(1)));
				} catch (NumberFormatException e) {
					System.out.println("Error in location file: " + line);
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br3.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br4 = null;
		try {
			try {
				br4 = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br4.readLine()) != null) {
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
				br4.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br5 = null;
		try {
			try {
				br5 = FileLoader.getReader(FileName.WIKIDATA_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br5.readLine()) != null) {
				String[] parts = line.split(Config.TAB);
				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br5.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\t" + wikidataIds.size() + " (with scientific articles)");

		// remove scientific articles
		// TODO: Do this as blacklist class

		Set<Entity> entitiesToRemove = new HashSet<Entity>();

		int entitiesToIgnore = 0;

		// Ignore all entities that are in WikiNews ONLY. Some events (e.g.
		// German federal election 2017 are events AND in WikiNews.
		Set<String> entitiesWithValidClass = new HashSet<String>();
		Set<String> entitiesInWikiNews = new HashSet<String>();

		BufferedReader br6 = null;
		try {
			try {
				br6 = FileLoader.getReader(FileName.WIKIDATA_INSTANCE_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br6.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				String parentClass = parts[2];

				if (parentClass.equals(WikidataResource.WIKINEWS_ARTICLE.getId())) {
					entitiesInWikiNews.add(parts[0]);
				} else if (parentClass.equals(WikidataResource.SCIENTIFIC_ARTICLE.getId())
						|| this.wikipediaInternalClasses.contains(parentClass)) {
					// remove scientific article and Wikipedia internal items
					// (e.g. templates)
					wikidataIds.remove(Integer.valueOf(parts[0].substring(1)));
					Entity entityToRemove = getEntityByWikidataId(parts[0]);
					if (entityToRemove != null) {
						entitiesToIgnore += 1;
						entitiesToRemove.add(entityToRemove);
					}
				} else {
					entitiesWithValidClass.add(parts[0]);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br6.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Ignore " + entitiesToIgnore
				+ " entities because they are Wikipedia internal entities or scientific article.");

		System.out.println("WikiNews entities: " + entitiesInWikiNews.size());
		entitiesInWikiNews.removeAll(entitiesWithValidClass);
		System.out.println("Ignore " + entitiesInWikiNews.size() + " entities because they are in WikiNews only.");
		entitiesWithValidClass.clear();

		for (String entity : entitiesInWikiNews) {
			wikidataIds.remove(Integer.valueOf(entity.substring(1)));
			Entity entityToRemove = getEntityByWikidataId(entity);
			if (entityToRemove != null) {
				entitiesToIgnore += 1;
				entitiesToRemove.add(entityToRemove);
			}
		}

		for (Entity entity : entitiesToRemove) {
			this.entitiesByWikidataNumericIds.remove(entity.getNumericWikidataId());
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				this.entitiesByWikipediaLabels.get(labelLanguage).remove(entity.getWikipediaLabel(labelLanguage));
				this.dataStore.removeEntity(entity);
				wikidataIds.remove(Integer.valueOf(entity.getNumericWikidataId()));
			}
		}

		System.out.println("\tWikidata IDs: " + wikidataIds.size());

		return wikidataIds;
	}

	private void loadWikidataIdMapping() {

		// wikipediaLabelsForWikidataIds = new HashMap<String, String>();
		// this.wikidataIdsForWikipediaLabels = new HashMap<Language,
		// Map<String, String>>();
		// this.wikidataEntitiesByIDs = new HashMap<Language, Map<String,
		// String>>();

		// this.entitiesByWikidataIds = new HashMap<String, Entity>();
		this.entitiesByWikipediaLabels = new HashMap<Language, Cache<String, Integer>>();

		MemoryStatsUtil.printMemoryStats();

		for (Language language : this.languages) {

			System.out.println("Load Wikidata mapping for the " + language.getLanguageAdjective()
					+ " Wikipedia in Wikidata - " + TimeTransformer.getTime() + ".");

			// this.entitiesByWikipediaLabels
			// .put(language,
			// cacheManager.createCache("entitiesByWikipediaLabels" +
			// language.getLanguageLowerCase(),
			// CacheConfigurationBuilder
			// .newCacheConfigurationBuilder(String.class, Entity.class,
			// ResourcePoolsBuilder.heap(10000).offheap(1, MemoryUnit.GB))
			// .build()));

			Cache<String, Integer> cache = this.cacheManager.createCache(
					"entitiesByWikipediaLabels" + language.getLanguageLowerCase(),
					CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Integer.class,
							ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100000, EntryUnit.ENTRIES)
									.offheap(3, MemoryUnit.GB).disk(20, MemoryUnit.GB, false)));

			this.entitiesByWikipediaLabels.put(language, cache);

			int lines = 0;
			long ms = System.currentTimeMillis();

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.ID_TO_WIKIPEDIA_MAPPING_FILE_NAME, language);
				while (it.hasNext()) {

					if (lines % 1000000 == 0) {
						long newMS = System.currentTimeMillis();

						System.out.println(lines + "\t" + language.getLanguage() + "\t" + TimeTransformer.getTime()
								+ " - " + (newMS - ms));
						ms = newMS;
					}

					String line = it.nextLine();

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

					// TODO: slow
					createEntity(wikidataId, language, wikipediaLabel, cache);
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

			System.out.println(language + ": " + lines + " labels.");

			MemoryStatsUtil.printMemoryStats();

		}

		System.out.println("STOP");
		System.exit(0);

		Set<Entity> entitiesToRemove = new HashSet<Entity>();
		// remove category entities
		for (Entity entity : this.dataStore.getEntities()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getCategoryPrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						entitiesToRemove.add(entity);
					}
				}
			}
		}
		System.out.println("Category entities to remove: " + entitiesToRemove.size());

		// remove list entities
		int removedListEntities = 0;
		for (Entity entity : this.dataStore.getEntities()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getListPrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						removedListEntities += 1;
						entitiesToRemove.add(entity);
					}
				}
			}
		}
		System.out.println("List entities to remove: " + removedListEntities);

		// remove template entities
		int removedTemplateEntities = 0;
		for (Entity entity : this.dataStore.getEntities()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getTemplatePrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						removedTemplateEntities += 1;
						entitiesToRemove.add(entity);
					}
				}
			}
		}
		System.out.println("Template entities to remove: " + removedTemplateEntities);

		for (Entity entity : entitiesToRemove) {
			this.entitiesByWikidataNumericIds.remove(entity.getNumericWikidataId());
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				this.entitiesByWikipediaLabels.get(labelLanguage).remove(entity.getWikipediaLabel(labelLanguage));
				this.dataStore.removeEntity(entity);
			}
		}

	}

	private Entity createEntity(String wikidataId, Language language, String wikipediaLabel,
			Cache<String, Integer> entitiesByWikipediaLabelsInLanguage) {

		int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

		Entity entity = this.entitiesByWikidataNumericIds.get(numericWikidataId);
		if (entity != null) {
			entity.addWikipediaLabel(language, wikipediaLabel);
		} else {
			entity = new Entity(language, wikipediaLabel, wikidataId, numericWikidataId);
			// this.dataStore.addEntity(entity);
			this.entitiesByWikidataNumericIds.put(numericWikidataId, entity);
		}

		entitiesByWikipediaLabelsInLanguage.put(wikipediaLabel, numericWikidataId);

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
		// if (!this.redirects.containsKey(language))
		// System.out.println("redirects is missing " + language);

		if (!this.entitiesByWikipediaLabels.containsKey(language))
			System.out.println("entitiesByWikipediaLabels is missing " + language);

		// if (this.redirects.get(language).containsKey(wikipediaLabel))
		// wikipediaLabel = this.redirects.get(language).get(wikipediaLabel);

		return entitiesByWikidataNumericIds.get(entitiesByWikipediaLabels.get(language).get(wikipediaLabel));
	}

	public Entity getEntityByWikidataId(String wikidataId) {
		return entitiesByWikidataNumericIds.get(Integer.parseInt(wikidataId.substring(1)));
	}

	public Entity getEntityByWikidataId(int wikidataId) {
		return entitiesByWikidataNumericIds.get(wikidataId);
	}

	public Cache<Integer, Entity> getEntitiesByWikidataIds() {
		return this.entitiesByWikidataNumericIds;
	}

	public Cache<Integer, Entity> getEntitiesByWikidataNumericIds() {
		return entitiesByWikidataNumericIds;
	}

	public void updateEntityToEvent(Entity entity, Event event) {
		this.entitiesByWikidataNumericIds.put(entity.getNumericWikidataId(), event);
		this.dataStore.removeEntity(entity);

		for (Language language : entity.getWikipediaLabels().keySet()) {
			entitiesByWikipediaLabels.get(language).put(entity.getWikipediaLabels().get(language),
					event.getNumericWikidataId());
		}
	}

	public void close() {
		this.cacheManager.close();
	}

}
