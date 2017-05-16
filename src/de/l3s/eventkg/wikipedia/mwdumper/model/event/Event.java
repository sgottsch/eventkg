package de.l3s.eventkg.wikipedia.mwdumper.model.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Event {

	private Date startDate;
	private Date endDate;
	private String categories;
	private String rawText;
	private String originalText;
	private PartialDate.Granularity granularity;

	private Set<String> links;

	public Event(Date startDate, Date endDate, String rawText) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.rawText = rawText;
		this.links = new HashSet<String>();
	}

	public Date getStartDate() {
		return this.startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return this.endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getCategories() {
		return this.categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public String getRawText() {
		return this.rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public PartialDate.Granularity getGranularity() {
		return this.granularity;
	}

	public void setGranularity(PartialDate.Granularity granularity) {
		this.granularity = granularity;
	}

	public String getOriginalText() {
		return this.originalText;
	}

	public void setOriginalText(String originalText) {
		this.originalText = originalText;
	}

	public String toString() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String res = "";
		if (this.startDate.equals(this.endDate)) {
			res = res + format.format(this.startDate);
		} else
			res = res + format.format(this.startDate) + "-" + format.format(this.endDate);
		res = res + ": " + this.rawText;
		return res;
	}

	public Set<String> getLinks() {
		return links;
	}

	public void addLink(String link) {
		this.links.add(link);
	}

}