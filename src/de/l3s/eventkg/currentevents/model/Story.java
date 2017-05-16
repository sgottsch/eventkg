package de.l3s.eventkg.currentevents.model;

public class Story {

	private long id;

	private String name;

	private String url;

	public Story(long id, String name, String url) {
		super();
		this.id = id;
		this.name = name;
		this.url = url;
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
