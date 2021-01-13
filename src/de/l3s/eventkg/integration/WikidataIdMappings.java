package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.db.DatabaseCreator;
import de.l3s.eventkg.integration.db.DatabaseName;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.source.dbpedia.DBpediaAllLocationsLoader;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.map.hash.THashMap;

public class WikidataIdMappings {

	private DatabaseCreator dbCreator = new DatabaseCreator();

	private Map<Language, Map<String, String>> wikidataPropertysByIDs;

	private Map<Integer, Entity> entitiesByWikidataNumericIds = new THashMap<Integer, Entity>();

	private List<Language> languages;

	private Map<String, TimeSymbol> temporalPropertyIds;

	public WikidataIdMappings(List<Language> languages) {
		this.languages = languages;
	}

	public void load() {
		loadEntitiesFromDB();

		loadWikidataPropertyIdMapping();
		loadTemporalProperties();
	}

	private void loadEntitiesFromDB() {

		System.out.println("Load entities from " + FileName.WIKIDATA_VALID_IDS.getFileName() + ".");
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.WIKIDATA_VALID_IDS);
			int i = 0;
			while (it.hasNext()) {
				if (i % 100000 == 0)
					System.out.println("Line " + i);
				i += 1;
				String line = it.nextLine();
				int wikidataId = Integer.valueOf(line);
				this.entitiesByWikidataNumericIds.put(Integer.valueOf(line), new Entity(wikidataId));
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
		System.out.println(" => Done.");

		System.out.println("Mark location entities.");
		DBpediaAllLocationsLoader.loadLocationEntities(this.languages, this);
		System.out.println(" => Done.");
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

		String wikidataId = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID),
				wikipediaLabel);

		if (wikidataId == null)
			return null;
		else
			return entitiesByWikidataNumericIds.get(Integer.valueOf(wikidataId));
	}

	public Event getEventByWikipediaLabel(Language language, String wikipediaLabel) {

		String wikidataId = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID),
				wikipediaLabel);

		Entity entity = entitiesByWikidataNumericIds.get(Integer.valueOf(wikidataId));

		if (entity != null && entity.isEvent())
			return (Event) entity;
		else
			return null;
	}

	public Entity getEntityByWikidataId(String wikidataId) {
		return entitiesByWikidataNumericIds.get(Integer.parseInt(wikidataId.substring(1)));
	}

	public Event getEventByWikidataId(String wikidataId) {
		Entity entity = entitiesByWikidataNumericIds.get(Integer.parseInt(wikidataId.substring(1)));
		if (entity != null && entity.isEvent())
			return (Event) entity;
		else
			return null;
	}

	public Entity getEntityByWikidataId(int wikidataId) {
		return entitiesByWikidataNumericIds.get(wikidataId);
	}

	public void updateEntityToEvent(Event event) {
		this.entitiesByWikidataNumericIds.put(event.getNumericWikidataId(), event);
		// this.dataStore.removeEntity(entity);

		// for (Language language : entity.getWikipediaLabels().keySet()) {
		// entitiesByWikipediaLabels.get(language).put(entity.getWikipediaLabels().get(language),
		// event.getNumericWikidataId());
		// }
	}

	public String getLabelsString(int numericWikidataId) {

		List<String> labels = new ArrayList<String>();

		for (Language language : this.languages) {
			String label = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIDATA_LABEL),
					String.valueOf(numericWikidataId));
			if (label == null)
				continue;
			labels.add(language.getLanguageLowerCase() + ": " + label);
		}

		return StringUtils.join(labels, " ");
	}

	public Map<Integer, Entity> getEntitiesByWikidataNumericIds() {
		return entitiesByWikidataNumericIds;
	}

	public Collection<Entity> getEntities() {
		return entitiesByWikidataNumericIds.values();
	}

}
