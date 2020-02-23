package de.l3s.eventkg.integration.db;

public enum DatabaseName {

	WIKIDATA_ID_TO_WIKIPEDIA_ID("WikidataIdToWikipediaId"),
	WIKIDATA_ID_TO_WIKIDATA_LABEL("WikidataIdToWikidataLabel"),
	WIKIPEDIA_ID_TO_WIKIDATA_ID("WikipediaIdToWikidataId"),
	WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID("WikidataIdToOldEventKGEventID", "eventkg_old"),
	WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID("WikidataIdToOldEventKGEntityID", "eventkg_old"),
	OLD_EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID("OldEventKGEventDescriptionToEventKGID", "eventkg_old"),
	WIKIDATA_ID_TO_EVENTKG_EVENT_ID("WikidataIdToEventKGEventID", "eventkg_current"),
	WIKIDATA_ID_TO_EVENTKG_ENTITY_ID("WikidataIdToEventKGEntityID", "eventkg_current"),
	EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID("EventKGEventDescriptionToEventKGID", "eventkg_current");

	private String name;
	private String subFolder;

	private DatabaseName(String name) {
		this.name = name;
	}

	private DatabaseName(String name, String subFolder) {
		this.name = name;
		this.subFolder = subFolder;
	}

	public String getName() {
		return name;
	}

	public String getSubFolder() {
		return subFolder;
	}

}