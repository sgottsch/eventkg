package de.l3s.eventkg.integration.model.relation;

import java.util.Set;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;
import de.l3s.eventkg.meta.Source;

public class GenericRelation extends RelationWithSource {

	private String property;
	private Set<SubProperty> properties;

	private Entity object;

	private Prefix prefix;

	private Source source;

	private Double weight;

	// private Map<Language, String> propertyLabels;

	private DateWithGranularity startTime;

	private DateWithGranularity endTime;

	// if the relation is neither temporal nor involves at least one event, mark
	// it here
	private boolean isEntityRelation;

	public GenericRelation() {
		super(null, null);
	}

	public GenericRelation(Entity subject, DataSet dataSet, Prefix prefix, String property, Entity object,
			Double weight, boolean isEntityRelation) {
		super(subject, dataSet);
		this.property = property;
		this.object = object;
		this.weight = weight;
		this.prefix = prefix;
		this.isEntityRelation = isEntityRelation;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public Set<SubProperty> getProperties() {
		return properties;
	}

	public void setProperties(Set<SubProperty> properties) {
		this.properties = properties;
	}

	public Entity getObject() {
		return object;
	}

	public void setObject(Entity object) {
		this.object = object;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	// public Map<Language, String> getPropertyLabels() {
	// return propertyLabels;
	// }
	//
	// public void setPropertyLabels(Map<Language, String> propertyLabels) {
	// this.propertyLabels = propertyLabels;
	// }

	public Prefix getPrefix() {
		return prefix;
	}

	public void setPrefix(Prefix prefix) {
		this.prefix = prefix;
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

	public boolean isEntityRelation() {
		return isEntityRelation;
	}

	public void setEntityRelation(boolean isEntityRelation) {
		this.isEntityRelation = isEntityRelation;
	}

}
