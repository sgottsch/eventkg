package de.l3s.eventkg.integration;

import java.util.HashSet;
import java.util.Set;

import com.sleepycat.je.Database;

import de.l3s.eventkg.integration.db.DatabaseCreator;
import de.l3s.eventkg.integration.db.DatabaseName;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.meta.Language;

public class EntityIdGenerator {

	private DatabaseName wikidataIdToEventKGEventID;
	private DatabaseName wikidataIdToEventKGEntityID;
	private DatabaseName eventKGDescriptionToWikidataId;

	private DatabaseCreator dbCreator = new DatabaseCreator();

	public EntityIdGenerator(boolean fromPreviousVersion) {

		System.out.println("EntityIdGenerator: " + fromPreviousVersion);

		if (fromPreviousVersion) {
			wikidataIdToEventKGEventID = DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID;
			wikidataIdToEventKGEntityID = DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID;
			eventKGDescriptionToWikidataId = DatabaseName.OLD_EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID;
		} else {
			wikidataIdToEventKGEventID = DatabaseName.WIKIDATA_ID_TO_EVENTKG_EVENT_ID;
			wikidataIdToEventKGEntityID = DatabaseName.WIKIDATA_ID_TO_EVENTKG_ENTITY_ID;
			eventKGDescriptionToWikidataId = DatabaseName.EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID;
		}
	}

	public int getLastEventNo() {
		return Integer
				.parseInt(dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEventID), EventKGDBCreator.LAST_ID));
	}

	public int getLastEntityNo() {
		return Integer
				.parseInt(dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEntityID), EventKGDBCreator.LAST_ID));
	}

	public String getID(Entity entity) {
		if (entity.isEvent())
			return getEventID((Event) entity);
		else
			return getEntityID(entity);
	}

	public String getEventID(Event event) {

		if (event.getId() != null)
			return event.getId();

		if (event.getWikidataId() == null && !event.getWikipediaLabels().isEmpty()) {
			System.out.println("ERROR: " + event.getWikidataLabels());
		}

		if (event.getWikidataId() != null) {
			return dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEventID),
					String.valueOf(event.getNumericWikidataId()));
		} else {
			Set<Integer> eventIds = new HashSet<Integer>();
			for (Description description : event.getDescriptions()) {
				Database db = this.dbCreator.getDB(description.getLanguage(), eventKGDescriptionToWikidataId);
				String id = this.dbCreator.getEntry(db, description.getLabel());
				if (id != null)
					eventIds.add(Integer.parseInt(id));
			}
			eventIds.remove(null);
			if (eventIds.size() == 1) {
				for (int eventId : eventIds) {
					return "event_" + eventId;
				}
			}
			return null;
		}
	}

	public String getEntityID(Entity entity) {
		if (entity.getId() != null)
			return entity.getId();

		return dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEntityID),
				String.valueOf(entity.getNumericWikidataId()));
	}

	public String getWikipediaId(Entity entity, Language language) {
		String res = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_ID),
				String.valueOf(entity.getNumericWikidataId()));
		return res;
	}

	public String getWikidataLabel(Entity entity, Language language) {
		return dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIDATA_LABEL),
				String.valueOf(entity.getNumericWikidataId()));
	}

	public String getEventKGEventIDByWikidataId(String wikidataId) {
		return dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEventID),
				String.valueOf(Integer.valueOf(wikidataId.substring(1))));
	}

	public String getEventKGIDByWikidataId(String wikidataId) {
		String id = dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEventID),
				String.valueOf(Integer.valueOf(wikidataId.substring(1))));
		if (id == null) {
			id = dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEntityID),
					String.valueOf(Integer.valueOf(wikidataId.substring(1))));
		}
		return id;
	}

	public String getEventIDByNumericWikidataId(int wikidataId) {
		return dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEventID), String.valueOf(wikidataId));
	}

	public String getEntityIDByNumericWikidataId(int wikidataId) {
		return dbCreator.getEntry(dbCreator.getDB(wikidataIdToEventKGEntityID), String.valueOf(wikidataId));
	}

	public String getEventIdByWikipediaId(Language language, String wikipediaId) {
		String wikidataId = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID),
				wikipediaId);
		if (wikidataId == null)
			return null;
		return getEventIDByNumericWikidataId(Integer.valueOf(wikidataId));
	}

	public String getEventKGIdByWikipediaId(Language language, String wikipediaId) {
		String wikidataId = dbCreator.getEntry(dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID),
				wikipediaId);

		if (wikidataId == null)
			return null;

		String id = getEventIDByNumericWikidataId(Integer.valueOf(wikidataId));

		if (id == null)
			id = getEntityIDByNumericWikidataId(Integer.valueOf(wikidataId));

		return id;
	}
}
