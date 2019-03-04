package de.l3s.eventkg.integration.model;

import java.util.Date;

public class DateWithGranularity {

	private Date date;

	private DateGranularity granularity;

	public DateWithGranularity(Date date, DateGranularity granularity) {
		super();
		this.date = date;
		this.granularity = granularity;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public DateGranularity getGranularity() {
		return granularity;
	}

	public void setGranularity(DateGranularity granularity) {
		this.granularity = granularity;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DateWithGranularity && this.date.equals(((DateWithGranularity) obj).getDate());
	}

	public boolean before(DateWithGranularity d) {
		return this.getDate().before(d.getDate());
	}

	public boolean after(DateWithGranularity d) {
		return this.getDate().after(d.getDate());
	}

}
