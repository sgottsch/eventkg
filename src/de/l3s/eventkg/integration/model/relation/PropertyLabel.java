package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.meta.Language;

/**
 * Relations can have several labels (e.g. Wikidata relation P361 has "is Part
 * of"@en, "Teil von"@de, ... Store these labels here to avoid storing the
 * labels on each relation instance.
 */

public class PropertyLabel {

	private Prefix prefix;

	private String property;

	private String label;

	private Language language;

	private DataSet dataSet;

	public PropertyLabel(Prefix prefix, String property, String label, Language language, DataSet dataSet) {
		super();
		this.prefix = prefix;
		this.property = property;
		this.label = label;
		this.language = language;
		this.dataSet = dataSet;
	}

	public Prefix getPrefix() {
		return prefix;
	}

	public String getProperty() {
		return property;
	}

	public String getLabel() {
		return label;
	}

	public Language getLanguage() {
		return language;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

}