package de.l3s.eventkg.integration.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileLoader;

public class Event extends Entity {

	private Set<Event> children = new HashSet<Event>();
	private Map<Event, Set<DataSet>> childrenWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> parents = new HashSet<Event>();
	private Map<Event, Set<DataSet>> parentsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> nextEvents = new HashSet<Event>();
	private Map<Event, Set<DataSet>> nextEventsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Event> previousEvents = new HashSet<Event>();
	private Map<Event, Set<DataSet>> previousEventsWithDataSets = new HashMap<Event, Set<DataSet>>();

	private Set<Entity> locations = new HashSet<Entity>();;
	private Map<Entity, Set<DataSet>> locationsWithDataSets = new HashMap<Entity, Set<DataSet>>();
	private Map<DataSet, Set<Entity>> dataSetsWithLocations = new HashMap<DataSet, Set<Entity>>();

	private Map<DataSet, Map<Language, Set<String>>> categories = new HashMap<DataSet, Map<Language, Set<String>>>();

	private DateWithGranularity startTime;

	private DateWithGranularity endTime;

	private Map<DataSet, Set<String>> otherUrls;

	private Set<String> eventInstanceComments = new HashSet<String>();

	private Map<DataSet, Set<String>> eventInstancesPerDataSet = new HashMap<DataSet, Set<String>>();

	private boolean isRecurring = false;
	private boolean isRecurrentEventEdition = false;

	private Map<DataSet, Set<String>> sources = new HashMap<DataSet, Set<String>>();

	private Map<Language, Set<String>> sentences = new HashMap<Language, Set<String>>();
	private Set<Description> descriptions = new HashSet<Description>();

	public Event() {
		super(null);
		setEvent(true);
	}

	public Event(String wikidataId) {
		super(wikidataId);
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel) {
		super(language, wikipediaLabel);
		setEvent(true);
	}

	public Event(Language language, String wikipediaLabel, String wikidataId) {
		super(language, wikipediaLabel, wikidataId);
		setEvent(true);
	}

	public Event(Entity entity, WikidataIdMappings wikidataIdMappings) {
		super(entity.getNumericWikidataId());
		// setYagoId(entity.getYagoId());

		// for (Language language : entity.getWikipediaLabels().keySet()) {
		// this.wikipediaLabels.put(language,
		// entity.getWikipediaLabels().get(language));
		// }
		//
		// for (Language language : entity.getWikidataLabels().keySet()) {
		// this.wikidataLabels.put(language,
		// entity.getWikidataLabels().get(language));
		// }

		setEvent(true);
		wikidataIdMappings.updateEntityToEvent(this);
		// DataStore.getInstance().removeEntity(entity);
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

	public DateWithGranularity getStartTime() {
		return startTime;
	}

	public void setStartTime(DateWithGranularity startTime) {
		this.startTime = startTime;
	}

	public DateWithGranularity getEndTime() {
		return endTime;
	}

	public void setEndTime(DateWithGranularity endTime) {
		this.endTime = endTime;
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

	public void addLocation(Entity location, DataSet dataSet) {
		this.locations.add(location);

		if (!this.locationsWithDataSets.containsKey(location))
			locationsWithDataSets.put(location, new HashSet<DataSet>());
		this.locationsWithDataSets.get(location).add(dataSet);

		if (!this.dataSetsWithLocations.containsKey(dataSet))
			dataSetsWithLocations.put(dataSet, new HashSet<Entity>());
		this.dataSetsWithLocations.get(dataSet).add(location);

	}

	public Set<Entity> getLocations() {
		return locations;
	}

	public Map<Entity, Set<DataSet>> getLocationsWithDataSets() {
		return locationsWithDataSets;
	}

	public Map<DataSet, Set<String>> getOtherUrls() {
		return otherUrls;
	}

	public void setOtherURLs(Map<DataSet, Set<String>> otherUrls) {
		this.otherUrls = otherUrls;
	}

	public Map<Event, Set<DataSet>> getChildrenWithDataSets() {
		return childrenWithDataSets;
	}

	public Map<Event, Set<DataSet>> getParentsWithDataSets() {
		return parentsWithDataSets;
	}

	public Set<String> getEventInstanceComments() {
		return eventInstanceComments;
	}

	public void addEventInstanceComment(String eventInstanceComment) {
		this.eventInstanceComments.add(eventInstanceComment);
	}

	public Map<Event, Set<DataSet>> getNextEventsWithDataSets() {
		return nextEventsWithDataSets;
	}

	public Map<Event, Set<DataSet>> getPreviousEventsWithDataSets() {
		return previousEventsWithDataSets;
	}

	public Map<DataSet, Set<String>> getEventInstancesPerDataSet() {
		return eventInstancesPerDataSet;
	}

	public void addDataSetAndEventInstance(DataSet dataSet, String eventInstance) {
		if (!this.eventInstancesPerDataSet.containsKey(dataSet))
			this.eventInstancesPerDataSet.put(dataSet, new HashSet<String>());
		this.eventInstancesPerDataSet.get(dataSet).add(eventInstance);
	}

	public Map<DataSet, Set<Entity>> getDataSetsWithLocations() {
		return dataSetsWithLocations;
	}

	public boolean isRecurring() {
		return isRecurring;
	}

	public void setRecurring(boolean isRecurring) {
		this.isRecurring = isRecurring;
	}

	public boolean isRecurrentEventEdition() {
		return isRecurrentEventEdition;
	}

	public void setRecurrentEventEdition(boolean isRecurrentEventEdition) {
		this.isRecurrentEventEdition = isRecurrentEventEdition;
	}

	public void addCategory(DataSet dataSet, Language language, String category) {
		if (!this.categories.containsKey(dataSet))
			this.categories.put(dataSet, new HashMap<Language, Set<String>>());
		if (!this.categories.get(dataSet).containsKey(language))
			this.categories.get(dataSet).put(language, new HashSet<String>());
		this.categories.get(dataSet).get(language).add(category);
	}

	public void addSource(DataSet dataSet, String source) {
		if (!this.sources.containsKey(dataSet))
			this.sources.put(dataSet, new HashSet<String>());
		this.sources.get(dataSet).add(source);
	}

	public Map<DataSet, Map<Language, Set<String>>> getCategories() {
		return categories;
	}

	public Map<DataSet, Set<String>> getSources() {
		return sources;
	}

	public Map<Language, Set<String>> getSentences() {
		return sentences;
	}

	public void addSentence(Language language, String sentence) {
		if (!this.sentences.containsKey(language))
			this.sentences.put(language, new HashSet<String>());
		this.sentences.get(language).add(sentence);
	}

	public Set<Description> getDescriptions() {
		return descriptions;
	}

	public void addDescription(DataSet dataSet, Language language, String description) {
		this.descriptions.add(new Description(dataSet, description, language));
	}

}
