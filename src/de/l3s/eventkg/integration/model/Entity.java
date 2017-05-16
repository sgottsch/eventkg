package de.l3s.eventkg.integration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;

public class Entity {

	protected Map<Language, String> wikipediaLabels = new HashMap<Language, String>();

	private String wikidataId;

	private String id;

	private boolean isEvent = false;

	private Event eventEntity;

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

}
