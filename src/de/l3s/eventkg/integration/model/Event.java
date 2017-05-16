package de.l3s.eventkg.integration.model;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileLoader;

public class Event extends Entity {

	private Set<Event> children;

	private Set<Event> parents;

	private Set<Entity> locations;
	private Map<Entity, Set<DataSet>> locationsWithDataSets = new HashMap<Entity, Set<DataSet>>();

	private Date startTime;

	private Date endTime;

	private Set<String> urls;

	public Event() {
		super(null);
		setEvent(true);
	}

	public Event(String wikidataId) {
		super(wikidataId);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		this.locations = new HashSet<Entity>();
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel) {
		super(language, wikipediaLabel);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		this.locations = new HashSet<Entity>();
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel, String wikidataId) {
		super(language, wikipediaLabel, wikidataId);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		this.locations = new HashSet<Entity>();
		setEvent(true);
	}

	public Event(Entity entity) {
		super(entity.getWikidataId());
		for (Language language : entity.getWikipediaLabels().keySet()) {
			this.wikipediaLabels.put(language, entity.getWikipediaLabels().get(language));
		}

		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		this.locations = new HashSet<Entity>();
		setEvent(true);
		entity.setEventEntity(this);
	}

	public Set<Event> getChildren() {
		return children;
	}

	public void setChildren(Set<Event> children) {
		this.children = children;
	}

	public Set<Event> getParents() {
		return parents;
	}

	public void setParents(Set<Event> parents) {
		this.parents = parents;
	}

	public void addParent(Event event) {
		this.parents.add(event);
	}

	public void addChild(Event event) {
		this.children.add(event);
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getOneLineRepresentation(List<Language> languages) {
		String oneLiner = this.getWikidataId() + " / " + this.getWikipediaLabelsString(languages);

		if (this.startTime != null && this.endTime != null) {
			if (!this.startTime.equals(this.endTime)) {
				oneLiner += " [" + FileLoader.PRINT_DATE_FORMAT.format(this.getStartTime()) + ","
						+ FileLoader.PRINT_DATE_FORMAT.format(this.getEndTime()) + "]";
			} else {
				oneLiner += " [" + FileLoader.PRINT_DATE_FORMAT.format(this.getStartTime()) + "]";
			}
		} else if (this.startTime != null) {
			oneLiner += " [" + FileLoader.PRINT_DATE_FORMAT.format(this.getStartTime()) + "]";
		} else if (this.endTime != null) {
			oneLiner += " [" + FileLoader.PRINT_DATE_FORMAT.format(this.getEndTime()) + "]";
		}

		return oneLiner;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public void addLocation(Entity location, DataSet dataSet) {
		this.locations.add(location);
		if (!this.locationsWithDataSets.containsKey(location))
			locationsWithDataSets.put(location, new HashSet<DataSet>());
		this.locationsWithDataSets.get(location).add(dataSet);
	}

	public Set<Entity> getLocations() {
		return locations;
	}

	public Map<Entity, Set<DataSet>> getLocationsWithDataSets() {
		return locationsWithDataSets;
	}

	public Set<String> getUrls() {
		return urls;
	}

	public void setURLs(Set<String> urls) {
		this.urls = urls;
	}

}
