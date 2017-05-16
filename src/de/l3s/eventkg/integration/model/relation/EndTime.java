package de.l3s.eventkg.integration.model.relation;

import java.util.Date;

import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class EndTime extends RelationWithSource {

	private Date endTime;

	private Source source;

	public EndTime(Event subject, DataSet dataSet, Date startTime) {
		super(subject, dataSet);
		this.endTime = startTime;
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
