package de.l3s.eventkg.integration;

import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.Description;
import de.l3s.eventkg.integration.model.relation.EndTime;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.Label;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class DataStore {

	private Set<Event> events = new THashSet<Event>();

	private Set<Entity> entities = new THashSet<Entity>();

	private Set<Alias> aliases = new THashSet<Alias>();

	private Set<Description> descriptions = new THashSet<Description>();

	private Set<EndTime> endTimes = new THashSet<EndTime>();

	private Set<StartTime> startTimes = new THashSet<StartTime>();

	private Set<Location> locations = new THashSet<Location>();

	private Set<GenericRelation> genericRelations = new THashSet<GenericRelation>();
	private Set<LiteralRelation> literalRelations = new THashSet<LiteralRelation>();

	private Set<Label> wikipediaLabels = new THashSet<Label>();
	private Set<Label> wikidataLabels = new THashSet<Label>();

	private Set<PropertyLabel> propertyLabels = new THashSet<PropertyLabel>();

	private Map<Entity, Set<Entity>> connectedEntities = new THashMap<Entity, Set<Entity>>();

	Map<Language, Map<Entity, Map<Entity, Integer>>> mentionCountsFromTextualEvents = new THashMap<Language, Map<Entity, Map<Entity, Integer>>>();

	private static DataStore instance;

	// private Map<Entity, Map<Entity, Set<GenericRelation>>>
	// linkRelationsBySubjectAndObject = new THashMap<Entity, Map<Entity,
	// Set<GenericRelation>>>();

	private Map<String, Set<GenericRelation>> linkRelationsBySubjectAndObjectGroup = new THashMap<String, Set<GenericRelation>>();

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

	public Set<LiteralRelation> getLiteralRelations() {
		return literalRelations;
	}

	public void setLiteralRelations(Set<LiteralRelation> literalRelations) {
		this.literalRelations = literalRelations;
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

	public void addLiteralRelation(LiteralRelation relation) {
		this.literalRelations.add(relation);
	}

	public void addGenericRelation(GenericRelation relation) {

		if (relation.getSubject().isEvent() || relation.getObject().isEvent()) {
			if (relation.getSubject().isEvent())
				relation.getObject().setActor(true);
			else
				relation.getSubject().setActor(true);
		}

		if (!relation.getSubject().isEvent() && !relation.getObject().isEvent()) {
			if (!this.connectedEntities.containsKey(relation.getSubject()))
				this.connectedEntities.put(relation.getSubject(), new THashSet<Entity>());
			if (!this.connectedEntities.containsKey(relation.getObject()))
				this.connectedEntities.put(relation.getObject(), new THashSet<Entity>());
			this.connectedEntities.get(relation.getSubject()).add(relation.getObject());
			this.connectedEntities.get(relation.getObject()).add(relation.getSubject());
		}

		this.genericRelations.add(relation);
	}

	// public void addLinkRelationOld(GenericRelation genericRelation) {
	//
	// if
	// (!linkRelationsBySubjectAndObject.containsKey(genericRelation.getSubject()))
	// linkRelationsBySubjectAndObject.put(genericRelation.getSubject(),
	// new THashMap<Entity, Set<GenericRelation>>());
	//
	// if
	// (!linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).containsKey(genericRelation.getObject()))
	// linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).put(genericRelation.getObject(),
	// new THashSet<GenericRelation>());
	//
	// linkRelationsBySubjectAndObject.get(genericRelation.getSubject()).get(genericRelation.getObject())
	// .add(genericRelation);
	// }

	public String addLinkRelation(GenericRelation genericRelation) {

		String subjectId = genericRelation.getSubject().getWikidataId();
		String objectId = genericRelation.getObject().getWikidataId();

		if (subjectId == null) {
			subjectId = genericRelation.getSubject().getTemporaryId();
		}

		String groupId = subjectId + "-" + objectId;

		if (!linkRelationsBySubjectAndObjectGroup.containsKey(groupId))
			linkRelationsBySubjectAndObjectGroup.put(groupId, new THashSet<GenericRelation>());

		linkRelationsBySubjectAndObjectGroup.get(groupId).add(genericRelation);

		return groupId;
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

	// public Map<Entity, Map<Entity, Set<GenericRelation>>>
	// getLinkRelationsBySubjectAndObject() {
	// return linkRelationsBySubjectAndObject;
	// }

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

	public Map<String, Set<GenericRelation>> getLinkRelationsBySubjectAndObjectGroup() {
		return linkRelationsBySubjectAndObjectGroup;
	}

}
