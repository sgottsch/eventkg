package de.l3s.eventkg.source.currentevents.model;

public class Story {

	private long id;

	private String name;

	private String wikipediaUrl;

	public Story(long id, String name, String url) {
		super();
		this.id = id;
		this.name = name;
		this.wikipediaUrl = url;
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

	public String getWikipediaUrl() {
		return wikipediaUrl;
	}

	public void setWikipediaUrl(String url) {
		this.wikipediaUrl = url;
	}

}
