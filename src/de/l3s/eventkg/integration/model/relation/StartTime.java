package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Source;

public class StartTime extends RelationWithSource {

	private DateWithGranularity startTime;

	private Source source;

	public StartTime(Entity subject, DataSet dataSet, DateWithGranularity startTime) {
		super(subject, dataSet);
		this.startTime = startTime;
		subject.addStartTime(startTime, dataSet);
	}

	public DateWithGranularity getStartTime() {
		return startTime;
	}

	public void setStartTime(DateWithGranularity startTime) {
		this.startTime = startTime;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
