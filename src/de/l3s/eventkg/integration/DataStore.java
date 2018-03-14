package de.l3s.eventkg.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Label;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;

public class DataStore {

	private Set<Event> events = new HashSet<Event>();

	private Set<Entity> entities = new HashSet<Entity>();

	private Set<Alias> aliases = new HashSet<Alias>();

	private Set<Description> descriptions = new HashSet<Description>();

	private Set<EndTime> endTimes = new HashSet<EndTime>();

	private Set<StartTime> startTimes = new HashSet<StartTime>();

	private Set<Location> locations = new HashSet<Location>();

	private Set<GenericRelation> genericRelations = new HashSet<GenericRelation>();

	private Set<GenericRelation> linkRelations = new HashSet<GenericRelation>();

	private Set<Label> wikipediaLabels = new HashSet<Label>();
	private Set<Label> wikidataLabels = new HashSet<Label>();

	private Set<PropertyLabel> propertyLabels = new HashSet<PropertyLabel>();

	private Map<Entity, Set<Entity>> connectedEntities = new HashMap<Entity, Set<Entity>>();

	Map<Language, Map<Entity, Map<Entity, Integer>>> mentionCountsFromTextualEvents = new HashMap<Language, Map<Entity, Map<Entity, Integer>>>();

	private static DataStore instance;

	private Map<Entity, Map<Entity, Set<GenericRelation>>> linkRelationsBySubjectAndObject = new HashMap<Entity, Map<Entity, Set<GenericRelation>>>();

	public static DataStore getInstance() {
		if (instance == null) {
			instance = new DataStore();
		}
		return instance;
	}

	private DataStore() {
	}

	public Set<Event> getEvents() {
		return events;
	}

	public Set<Entity> getEntities() {
		return entities;
	}

	public Set<Alias> getAliases() {
		return aliases;
	}

	public Set<Description> getDescriptions() {
		return descriptions;
	}

	public Set<EndTime> getEndTimes() {
		return endTimes;
	}

	public Set<StartTime> getStartTimes() {
		return startTimes;
	}

	public Set<Location> getLocations() {
		return locations;
	}

	public Set<GenericRelation> getGenericRelations() {
		return genericRelations;
	}

	public Set<GenericRelation> getLinkRelations() {
		return linkRelations;
	}

	public Set<Label> getWikipediaLabels() {
		return wikipediaLabels;
	}

	public void addEvent(Event event) {
		this.events.add(event);
	}

	public void addWikipediaLabel(Label label) {
		this.wikipediaLabels.add(label);
	}

	public Set<Label> getWikidataLabels() {
		return wikidataLabels;
	}

	public void addWikidataLabel(Label label) {
		this.wikidataLabels.add(label);
	}

	public void addAlias(Alias alias) {
		this.aliases.add(alias);
	}

	public void addDescription(Description description) {
		if (description.getLabel().startsWith("[["))
			return;
		this.descriptions.add(description);
	}

	public void addGenericRelation(GenericRelation relation) {

		if (relation.getSubject().isEvent() || relation.getObject().isEvent()) {
			if (relation.getSubject().isEvent())
				relation.getObject().setActor(true);
			else
				relation.getSubject().setActor(true);
		}

		if (!relation.getSubject().isEvent() && !relation.getObject().isEvent()) {

			if (this.connectedEntities.size() < 50) {
				System.out.println("connected entities: " + relation.getSubject().getWikidataId() + "\t"
						+ relation.getObject().getWikidataId());
			}

			if (!this.connectedEntities.containsKey(relation.getSubject()))
				this.connectedEntities.put(relation.getSubject(), new HashSet<Entity>());
			if (!this.connectedEntities.containsKey(relation.getObject()))
				this.connectedEntities.put(relation.getObject(), new HashSet<Entity>());
			this.connectedEntities.get(relation.getSubject()).add(relation.getObject());
			this.connectedEntities.get(relation.getObject()).add(relation.getSubject());
		}

		this.genericRelations.add(relation);
	}

	public void addLinkRelation(GenericRelation genericRelation) {

		if (!linkRelationsBySubjectAndObject.containsKey(genericRelation.getSubject()))
			linkRelationsBySubjectAndObject.put(genericRelation.getSubject(),
					new HashMap<Entity, Set<GenericRelation>>());

		if (!linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).containsKey(genericRelation.getObject()))
			linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).put(genericRelation.getObject(),
					new HashSet<GenericRelation>());

		linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).get(genericRelation.getObject())
				.add(genericRelation);

		this.linkRelations.add(genericRelation);
	}

	public void addEndTime(EndTime endTime) {
		this.endTimes.add(endTime);
	}

	public void addStartTime(StartTime startTime) {
		this.startTimes.add(startTime);
	}

	public void addLocation(Location location) {
		this.locations.add(location);
	}

	public void addEntity(Entity entity) {
		this.entities.add(entity);
	}

	public void removeEntity(Entity entity) {
		this.entities.remove(entity);
	}

	public void addPropertyLabel(PropertyLabel propertyLabel) {
		this.propertyLabels.add(propertyLabel);
	}

	public Set<PropertyLabel> getPropertyLabels() {
		return propertyLabels;
	}

	public Map<Entity, Map<Entity, Set<GenericRelation>>> getLinkRelationsBySubjectAndObject() {
		return linkRelationsBySubjectAndObject;
	}

	public Map<Entity, Set<Entity>> getConnectedEntities() {
		return connectedEntities;
	}

	public Map<Language, Map<Entity, Map<Entity, Integer>>> getMentionCountsFromTextualEvents() {
		return mentionCountsFromTextualEvents;
	}

	public void setMentionCountsFromTextualEvents(
			Map<Language, Map<Entity, Map<Entity, Integer>>> mentionCountsFromTextualEvents) {
		this.mentionCountsFromTextualEvents = mentionCountsFromTextualEvents;
	}

}
