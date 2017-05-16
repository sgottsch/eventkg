package de.l3s.eventkg.currentevents.model;

import java.util.Date;
import java.util.Set;

public class WCEEvent {

	private long id;

	private Date date;

	private String description;

	private Category category;

	private Set<WCEEntity> entities;

	private Set<Source> sources;

	private String method;

	private Story story;
	
	public WCEEvent(Date date, String description, Category category, Set<WCEEntity> entities, Set<Source> sources) {
		this.date = date;
		this.description = description;
		this.category = category;
		this.entities = entities;
		this.sources = sources;
	}

	public WCEEvent(Long id, Date date, String description, Category category, Story story, Set<WCEEntity> entities, Set<Source> sources) {
		this.id = id;
		this.date = date;
		this.description = description;
		this.category = category;
		this.story = story;
		this.entities = entities;
		this.sources = sources;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<WCEEntity> getEntities() {
		return entities;
	}

	public void setEntities(Set<WCEEntity> entities) {
		this.entities = entities;
	}

	public Set<Source> getSources() {
		return sources;
	}

	public void setSources(Set<Source> sources) {
		this.sources = sources;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Story getStory() {
		return story;
	}

	public void setStory(Story story) {
		this.story = story;
	}

}
