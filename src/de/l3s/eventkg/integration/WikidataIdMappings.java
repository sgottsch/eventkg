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
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.wikipedia.WikiWords;

public class WikidataIdMappings {

	// private Map<Language, Map<String, String>> wikidataEntitiesByIDs;
	// private Map<Language, Map<String, String>> wikidataIdsForWikipediaLabels;
	private Map<Language, Map<String, String>> wikidataPropertysByIDs;

	private Map<String, Entity> entitiesByWikidataIds;
	private Map<Language, Map<String, Entity>> entitiesByWikipediaLabels;

	private List<Language> languages;

	// private Set<String> wikidataIdsThatHaveLabels = new HashSet<String>();

	private Map<String, TemporalPropertyType> temporalPropertyIds;

	public WikidataIdMappings(List<Language> languages) {
		this.languages = languages;
	}

	public void load() {
		loadWikidataIdMapping();
		loadWikidataPropertyIdMapping();
		loadTemporalProperties();
	}

	// private void loadWikidataLabels() {
	//
	// for (Language language : this.languages) {
	//
	// BufferedReader br = null;
	// try {
	// br = FileLoader.getReader(FileName.WIKIDATA_LABELS, language);
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	//
	// String[] parts = line.split("\t");
	//
	// if(parts[0].equals("Q17703499"))
	// System.out.println("STRANGE");
	//
	// wikidataIdsThatHaveLabels.add(parts[0]);
	//
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// br.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	//
	// }
	//
	// }

	private void loadWikidataIdMapping() {

		// wikipediaLabelsForWikidataIds = new HashMap<String, String>();
		// this.wikidataIdsForWikipediaLabels = new HashMap<Language,
		// Map<String, String>>();
		// this.wikidataEntitiesByIDs = new HashMap<Language, Map<String,
		// String>>();

		this.entitiesByWikidataIds = new HashMap<String, Entity>();
		this.entitiesByWikipediaLabels = new HashMap<Language, Map<String, Entity>>();

		for (Language language : this.languages) {

			System.out.println("Load Wikidata mapping for the " + language.getLanguageAdjective()
					+ " Wikipedia labels in Wikidata.");

			this.entitiesByWikipediaLabels.put(language, new HashMap<String, Entity>());

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
		}

		Set<Entity> listEntities = new HashSet<Entity>();
		// remove list entities
		for (Entity entity : this.entitiesByWikidataIds.values()) {
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				for (String listPrefix : WikiWords.getInstance().getListPrefixes(labelLanguage)) {
					if (entity.getWikipediaLabel(labelLanguage).startsWith(listPrefix)) {
						listEntities.add(entity);
					}
				}
			}
		}
		for (Entity entity : listEntities) {
			this.entitiesByWikidataIds.remove(entity.getWikidataId());
			for (Language labelLanguage : entity.getWikipediaLabels().keySet()) {
				this.entitiesByWikipediaLabels.get(labelLanguage).remove(entity.getWikipediaLabel(labelLanguage));
			}
		}

	}

	private Entity createEntity(String wikidataId, Language language, String wikipediaLabel) {

		Entity entity = this.entitiesByWikidataIds.get(wikidataId);
		if (entity != null) {
			entity.addWikipediaLabel(language, wikipediaLabel);
		} else {
			entity = new Entity(language, wikipediaLabel, wikidataId);
			DataStore.getInstance().addEntity(entity);
			this.entitiesByWikidataIds.put(wikidataId, entity);
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

	public TemporalPropertyType getWikidataTemporalPropertyTypeById(String propertyId) {
		return this.temporalPropertyIds.get(propertyId);
	}

	public void loadTemporalProperties() {
		this.temporalPropertyIds = new HashMap<String, TemporalPropertyType>();

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
				String type = parts[2];

				TemporalPropertyType propType = null;
				switch (type) {
				case "n":
					propType = TemporalPropertyType.NONE;
					break;
				case "s":
					propType = TemporalPropertyType.START;
					break;
				case "e":
					propType = TemporalPropertyType.END;
					break;
				case "b":
					propType = TemporalPropertyType.BOTH;
					break;
				default:
					break;
				}

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
		return entitiesByWikipediaLabels.get(language).get(wikipediaLabel);
	}

	public Entity getEntityByWikidataId(String wikidataId) {
		return entitiesByWikidataIds.get(wikidataId);
	}

	public Map<String, Entity> getEntitiesByWikidataIds() {
		return entitiesByWikidataIds;
	}

	public enum TemporalPropertyType {
		START,
		END,
		NONE,
		BOTH;
	}

	// public Set<String> getWikidataIdsThatHaveLabels() {
	// return wikidataIdsThatHaveLabels;
	// }

}
