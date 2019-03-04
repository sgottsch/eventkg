package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Source;

public class EndTime extends RelationWithSource {

	private DateWithGranularity endTime;

	private Source source;

	public EndTime(Entity subject, DataSet dataSet, DateWithGranularity endTime) {
		super(subject, dataSet);
		this.endTime = endTime;
		subject.addEndTime(endTime, dataSet);
	}

	public DateWithGranularity getEndTime() {
		return endTime;
	}

	public void setEndTime(DateWithGranularity startTime) {
		this.endTime = startTime;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
