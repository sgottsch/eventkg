package de.l3s.eventkg.source.currentevents.model;

public class Source {

	private long id;

	private String url;

	private String type;

	private String sourceName;

	public Source(long id, String uri, String type, String sourceName) {
		this.id = id;
		this.url = uri;
		this.type = type;
		this.sourceName = sourceName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String uri) {
		this.url = uri;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

}
