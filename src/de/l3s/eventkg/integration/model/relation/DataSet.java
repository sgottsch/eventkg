package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.meta.Source;

public class DataSet {

	private Source source;

	private String date;

	private String id;

	private String url;

	public DataSet(Source source, String id, String url) {
		super();
		this.source = source;
		this.id = id;
		this.url = url;
	}

	public String getId() {
		if (this.id == null) {
			id = source.toString().toLowerCase() + "_" + date;
		}
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
