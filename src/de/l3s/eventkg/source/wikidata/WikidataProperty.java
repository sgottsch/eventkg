package de.l3s.eventkg.source.wikidata;

public enum WikidataProperty {

	INSTANCE_OF("P31"),
	SUB_CLASS("P279"),
	PART_OF("P361"),
	FOLLOWS("P155"),
	FOLLOWED_BY("P156"),
	EDITION_NUMBER("P393"),
	SPORTS_SEASON_OF_LEAGUE_OR_COMPETITION("P3450"),
	SEASON_OF_CLUB_OR_TEAM("P5138"),
	PART_OF_THE_SERIES("P179");

	private String id;

	WikidataProperty(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
