package de.l3s.eventkg.source.currentevents.model;

public class WCEEntity {

	private long id;

	private String wikiURL;

	public WCEEntity(String wikiURL) {
		this.wikiURL = wikiURL;
	}

	public WCEEntity(long id, String wikiURL) {
		this.id = id;
		this.wikiURL = wikiURL;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getWikiURL() {
		return wikiURL;
	}

	public void setWikiURL(String wikiURL) {
		this.wikiURL = wikiURL;
	}

}
