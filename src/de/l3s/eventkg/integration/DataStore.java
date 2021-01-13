package de.l3s.eventkg.integration;

import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.meta.Language;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class DataStore {

	private Set<Event> events = new THashSet<Event>();

	private Set<GenericRelation> genericRelations = new THashSet<GenericRelation>();
	private Set<LiteralRelation> literalRelations = new THashSet<LiteralRelation>();

	private Set<PropertyLabel> propertyLabels = new THashSet<PropertyLabel>();

	Map<Language, Map<Entity, Map<Entity, Integer>>> mentionCountsFromTextualEvents = new THashMap<Language, Map<Entity, Map<Entity, Integer>>>();

	private static DataStore instance;

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

	public Set<GenericRelation> getGenericRelations() {
		return genericRelations;
	}

	public Set<LiteralRelation> getLiteralRelations() {
		return literalRelations;
	}

	public void setLiteralRelations(Set<LiteralRelation> literalRelations) {
		this.literalRelations = literalRelations;
	}

	public void addEvent(Event event) {
		this.events.add(event);
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
		this.genericRelations.add(relation);
	}

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

	public void addPropertyLabel(PropertyLabel propertyLabel) {
		this.propertyLabels.add(propertyLabel);
	}

	public Set<PropertyLabel> getPropertyLabels() {
		return propertyLabels;
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

	public void clearLiteralRelations() {
		this.literalRelations = null;
	}

	public void clearGenericRelations() {
		this.genericRelations = null;
	}

}
