package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.relation.prefix.Prefix;

public class SubProperty {

	private String property;

	private DataSet dataSet;

	private Prefix prefix;

	public SubProperty(String property, DataSet dataSet, Prefix prefix) {
		super();
		this.property = property;
		this.dataSet = dataSet;
		this.prefix = prefix;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	public Prefix getPrefix() {
		return prefix;
	}

	public void setPrefix(Prefix prefix) {
		this.prefix = prefix;
	}

}
