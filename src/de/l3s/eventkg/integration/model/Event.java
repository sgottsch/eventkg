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
	private Map<Event, Set<DataSet>> childrenWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> parents;
	private Map<Event, Set<DataSet>> parentsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> nextEvents = new HashSet<Event>();
	private Map<Event, Set<DataSet>> nextEventsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> previousEvents = new HashSet<Event>();
	private Map<Event, Set<DataSet>> previousEventsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Entity> locations = new HashSet<Entity>();;
	private Map<Entity, Set<DataSet>> locationsWithDataSets = new HashMap<Entity, Set<DataSet>>();

	private Date startTime;

	private Date endTime;

	private Set<String> urls;

	private Set<String> otherUrls;

	public Event() {
		super(null);
		setEvent(true);
	}

	public Event(String wikidataId) {
		super(wikidataId);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel) {
		super(language, wikipediaLabel);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel, String wikidataId) {
		super(language, wikipediaLabel, wikidataId);
		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
		setEvent(true);
	}

	public Event(Entity entity) {
		super(entity.getWikidataId());
		for (Language language : entity.getWikipediaLabels().keySet()) {
			this.wikipediaLabels.put(language, entity.getWikipediaLabels().get(language));
		}

		this.children = new HashSet<Event>();
		this.parents = new HashSet<Event>();
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

	public Set<Event> getNextEvents() {
		return nextEvents;
	}

	public void setNextEvents(Set<Event> nextEvents) {
		this.nextEvents = nextEvents;
	}

	public Set<Event> getPreviousEvents() {
		return previousEvents;
	}

	public void setPreviousEvents(Set<Event> previousEvents) {
		this.previousEvents = previousEvents;
	}

	public void addParent(Event event, DataSet dataSet) {
		this.parents.add(event);
		if (!this.parentsWithDataSets.containsKey(event))
			parentsWithDataSets.put(event, new HashSet<DataSet>());
		this.parentsWithDataSets.get(event).add(dataSet);
	}

	public void addChild(Event event, DataSet dataSet) {
		this.children.add(event);
		if (!this.childrenWithDataSets.containsKey(event))
			childrenWithDataSets.put(event, new HashSet<DataSet>());
		this.childrenWithDataSets.get(event).add(dataSet);
	}

	public void addNextEvent(Event event, DataSet dataSet) {
		this.nextEvents.add(event);
		if (!this.nextEventsWithDataSets.containsKey(event))
			nextEventsWithDataSets.put(event, new HashSet<DataSet>());
		this.nextEventsWithDataSets.get(event).add(dataSet);
	}

	public void addPreviousEvent(Event event, DataSet dataSet) {
		this.previousEvents.add(event);
		if (!this.previousEventsWithDataSets.containsKey(event))
			previousEventsWithDataSets.put(event, new HashSet<DataSet>());
		this.previousEventsWithDataSets.get(event).add(dataSet);
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

	public Set<String> getOtherUrls() {
		return otherUrls;
	}

	public void setOtherURLs(Set<String> otherUrls) {
		this.otherUrls = otherUrls;
	}

	public Map<Event, Set<DataSet>> getChildrenWithDataSets() {
		return childrenWithDataSets;
	}

	public Map<Event, Set<DataSet>> getParentsWithDataSets() {
		return parentsWithDataSets;
	}

}
