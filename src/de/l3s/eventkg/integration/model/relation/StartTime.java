package de.l3s.eventkg.integration.model.relation;

import java.util.Date;

import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Source;

public class StartTime extends RelationWithSource {

	private Date startTime;

	private Source source;

	public StartTime(Event subject, DataSet dataSet, Date startTime) {
		super(subject, dataSet);
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
