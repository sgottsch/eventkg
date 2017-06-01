package de.l3s.eventkg.integration.model.relation;

import java.util.Map;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class GenericRelation extends RelationWithSource {

	private String property;

	private Entity object;

	private Prefix prefix;

	private Source source;

	private Double weight;

	private Map<Language, String> propertyLabels;

	public GenericRelation(Entity subject, DataSet dataSet, Prefix prefix, String property, Entity object,
			Double weight) {
		super(subject, dataSet);
		this.property = property;
		this.object = object;
		this.weight = weight;
		this.prefix = prefix;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
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

	public Map<Language, String> getPropertyLabels() {
		return propertyLabels;
	}

	public void setPropertyLabels(Map<Language, String> propertyLabels) {
		this.propertyLabels = propertyLabels;
	}

	public Prefix getPrefix() {
		return prefix;
	}

	public void setPrefix(Prefix prefix) {
		this.prefix = prefix;
	}

}
