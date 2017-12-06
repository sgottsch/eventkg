package de.l3s.eventkg.integration.model;

import java.util.Date;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class Relation {

	private Entity entity1;

	private String property;

	private Entity entity2;

	private Date startTime;

	private Date endTime;
	private Date startTimeEntity1;

	private Date endTimeEntity1;

	private Date startTimeEntity2;

	private Date endTimeEntity2;

	private Source source;

	private Language sourceLanguage;

	// if the relation is neither temporal nor involves at least one event, mark
	// it here
	private boolean isEntityRelation;

	public Relation(Entity entity1, Entity entity2, Date startTime, Date endTime, String property, Source source,
			Language sourceLanguage, boolean isEntityRelation) {
		super();
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.startTime = startTime;
		this.endTime = endTime;
		this.property = property;
		this.source = source;
		this.sourceLanguage = sourceLanguage;
	}

	public Entity getEntity1() {
		return entity1;
	}

	public void setEntity1(Entity entity1) {
		this.entity1 = entity1;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public Entity getEntity2() {
		return entity2;
	}

	public void setEntity2(Entity entity2) {
		this.entity2 = entity2;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Date getStartTimeEntity1() {
		return startTimeEntity1;
	}

	public void setStartTimeEntity1(Date startTimeEntity1) {
		this.startTimeEntity1 = startTimeEntity1;
	}

	public Date getEndTimeEntity1() {
		return endTimeEntity1;
	}

	public void setEndTimeEntity1(Date endTimeEntity1) {
		this.endTimeEntity1 = endTimeEntity1;
	}

	public Date getStartTimeEntity2() {
		return startTimeEntity2;
	}

	public void setStartTimeEntity2(Date startTimeEntity2) {
		this.startTimeEntity2 = startTimeEntity2;
	}

	public Date getEndTimeEntity2() {
		return endTimeEntity2;
	}

	public void setEndTimeEntity2(Date endTimeEntity2) {
		this.endTimeEntity2 = endTimeEntity2;
	}

	public Language getSourceLanguage() {
		return sourceLanguage;
	}

	public void setSourceLanguage(Language sourceLanguage) {
		this.sourceLanguage = sourceLanguage;
	}

	public boolean isEntityRelation() {
		return isEntityRelation;
	}

}
