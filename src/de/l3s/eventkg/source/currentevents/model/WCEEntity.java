package de.l3s.eventkg.source.currentevents.model;

public class WCEEntity {

	private long id;

	private String name;

	private String wikiURL;

	private String method;

	public WCEEntity(String name, String wikiURL) {
		this.name = name;
		this.wikiURL = wikiURL;
	}

	public WCEEntity(long id, String name, String wikiURL) {
		this.id = id;
		this.name = name;
		this.wikiURL = wikiURL;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getWikiURL() {
		return wikiURL;
	}

	public void setWikiURL(String wikiURL) {
		this.wikiURL = wikiURL;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

}
