package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.Entity;

public abstract class RelationWithSource {

	private Entity subject;

	private DataSet dataSet;

	public RelationWithSource(Entity subject, DataSet dataSet) {
		super();
		this.subject = subject;
		this.dataSet = dataSet;
	}

	public Entity getSubject() {
		return subject;
	}

	public void setSubject(Entity subject) {
		this.subject = subject;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

}
