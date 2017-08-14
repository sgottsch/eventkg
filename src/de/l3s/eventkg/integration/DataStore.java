package de.l3s.eventkg.integration;

import java.util.HashSet;
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

	private Set<Label> labels = new HashSet<Label>();

	private Set<PropertyLabel> propertyLabels = new HashSet<PropertyLabel>();

	private static DataStore instance;
	
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

	public Set<Label> getLabels() {
		return labels;
	}

	public void addEvent(Event event) {
		this.events.add(event);
	}

	public void addLabel(Label label) {
		this.labels.add(label);
	}

	public void addAlias(Alias alias) {
		this.aliases.add(alias);
	}

	public void addDescription(Description description) {
		this.descriptions.add(description);
	}

	public void addGenericRelation(GenericRelation genericRelation) {
		this.genericRelations.add(genericRelation);
	}

	public void addLinkRelation(GenericRelation genericRelation) {
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

}
