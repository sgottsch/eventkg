package de.l3s.eventkg.integration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;

public class Entity {

	protected Map<Language, String> wikipediaLabels = new HashMap<Language, String>();

	private String wikidataId;

	private String id;

	private boolean isEvent = false;

	private Set<Entity> subLocations = new HashSet<Entity>();

	private Set<Entity> parentLocations = new HashSet<Entity>();
	private Set<Entity> allParentLocations = new HashSet<Entity>();

	private Event eventEntity;

	private boolean isLocation = false;

	public Entity(String wikidataId) {
		super();
		this.wikidataId = wikidataId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Entity(Language language, String wikipediaLabel) {
		super();
		this.wikipediaLabels.put(language, wikipediaLabel);
	}

	public Entity(Language language, String wikipediaLabel, String wikidataId) {
		super();
		this.wikipediaLabels.put(language, wikipediaLabel);
		this.wikidataId = wikidataId;
	}

	public String getWikipediaLabel(Language language) {
		return wikipediaLabels.get(language);
	}

	public void addWikipediaLabel(Language language, String wikipediaLabel) {
		this.wikipediaLabels.put(language, wikipediaLabel);
	}

	public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public boolean isEvent() {
		return isEvent;
	}

	public void setEvent(boolean isEvent) {
		this.isEvent = isEvent;
	}

	public String getWikipediaLabelsString(List<Language> languages) {
		List<String> labels = new ArrayList<String>();
		for (Language language : languages) {
			if (this.wikipediaLabels.containsKey(language))
				labels.add(language.getLanguageLowerCase() + ":" + this.wikipediaLabels.get(language));
			else
				labels.add(language.getLanguageLowerCase() + ":-");
		}
		return StringUtils.join(labels, " ");
	}

	public Event getEventEntity() {
		return eventEntity;
	}

	public void setEventEntity(Event eventEntity) {
		this.eventEntity = eventEntity;
	}

	public Map<Language, String> getWikipediaLabels() {
		return wikipediaLabels;
	}

	public Set<Entity> getSubLocations() {
		return subLocations;
	}

	public void addSubLocation(Entity subLocation) {
		this.subLocations.add(subLocation);
	}

	public Set<Entity> getParentLocations() {
		return parentLocations;
	}

	public void addParentLocation(Entity parentLocation) {
		this.parentLocations.add(parentLocation);
	}

	public boolean isLocation() {
		return isLocation;
	}

	public void setLocation(boolean isLocation) {
		this.isLocation = isLocation;
	}

	public Set<Entity> getAllParentLocations() {
		return allParentLocations;
	}

	public void addAllParentLocation(Entity allParentLocation) {
		this.allParentLocations.add(allParentLocation);
	}

}
