package de.l3s.eventkg.source.currentevents.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.source.currentevents.model.Category;
import de.l3s.eventkg.source.currentevents.model.Source;
import de.l3s.eventkg.source.currentevents.model.Story;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;

public class DataStore {

	private Map<Long, WCEEntity> entities;

	private Map<Long, WCEEvent> events;

	private Map<Long, Source> sources;

	private Map<Long, Category> categories;

	private Map<Long, Story> stories;

	private Map<String, WCEEntity> entitiesPerURI;

	public DataStore() {
		this.entities = new HashMap<Long, WCEEntity>();
		this.events = new HashMap<Long, WCEEvent>();
		this.entitiesPerURI = new HashMap<String, WCEEntity>();
		this.sources = new HashMap<Long, Source>();
		this.categories = new HashMap<Long, Category>();
		this.stories = new HashMap<Long, Story>();
	}

	public WCEEntity getEntity(Long id, String name, String url) {
		if (!this.entities.containsKey(id)) {
			WCEEntity entity = new WCEEntity(id, name, url);
			entity.setMethod("ev");
			this.entities.put(id, entity);
			return entity;
		}
		return this.entities.get(id);
	}

	public Source getSource(Long id, String url, String type, String sourceName) {
		if (!this.sources.containsKey(id)) {
			Source source = new Source(id, url, type, sourceName);
			this.sources.put(id, source);
			return source;
		}
		return this.sources.get(id);
	}

	public Category getCategory(Long id, String name) {
		if (!this.categories.containsKey(id)) {
			Category category = new Category(id, name);
			this.categories.put(id, category);
			return category;
		}
		return this.categories.get(id);
	}

	public Set<WCEEntity> getEntities() {
		return new HashSet<WCEEntity>(entities.values());
	}

	public Set<Source> getSources() {
		return new HashSet<Source>(sources.values());
	}

	public Map<Long, Category> getCategoriesByID() {
		return categories;
	}

	public Set<Category> getCategories() {
		return new HashSet<Category>(categories.values());
	}

	public Story getStory(long id, String name, String url) {
		if (!this.stories.containsKey(id)) {
			Story story = new Story(id, name, url);
			this.stories.put(id, story);
			return story;
		}
		return this.stories.get(id);
	}

	public Set<Story> getStories() {
		return new HashSet<Story>(stories.values());
	}

	public Map<String, WCEEntity> getEntitiesPerURI() {
		return entitiesPerURI;
	}

	public Map<Long, WCEEntity> getEntitiesByID() {
		return this.entities;
	}

	public Map<Long, Source> getSourcesByID() {
		return this.sources;
	}

	public Map<Long, Story> getStoriesByID() {
		return this.stories;
	}

	public Map<Long, WCEEvent> getEventsByID() {
		return this.events;
	}

	public Set<WCEEvent> getEvents() {
		return new HashSet<WCEEvent>(events.values());
	}

}
