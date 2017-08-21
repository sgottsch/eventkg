package de.l3s.eventkg.integration.model.relation;

import java.util.Date;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Source;

public class EndTime extends RelationWithSource {

	private Date endTime;

	private Source source;

	public EndTime(Entity subject, DataSet dataSet, Date endTime) {
		super(subject, dataSet);
		this.endTime = endTime;
		subject.addEndTime(endTime, dataSet);
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date startTime) {
		this.endTime = startTime;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
