package de.l3s.eventkg.source.wikidata;

public enum WikidataProperty {

	INSTANCE_OF("P31"),
	SUB_CLASS("P279"),
	PART_OF("P361"),
	FOLLOWS("P155"),
	FOLLOWED_BY("P156"),;

	private String id;

	WikidataProperty(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
