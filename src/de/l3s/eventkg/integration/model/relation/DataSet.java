package de.l3s.eventkg.integration.model.relation;

import java.util.Date;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class DataSet {

	private Source source;

	private Date date;

	private String id;

	private String url;

	private Language language;

	public DataSet(Source source, String id, String url, Language language) {
		super();
		this.source = source;
		this.id = id;
		this.url = url;
		this.language = language;
	}

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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

}
