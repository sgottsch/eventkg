package de.l3s.eventkg.integration.model.relation;

import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.StatementRank;

import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.prefix.Prefix;

public class LiteralRelation extends RelationWithSource {

	private String property;
	private Set<SubProperty> properties = null;

	private String object;

	private LiteralDataType dataType;
	private String languageCode;

	private DateWithGranularity startTime;

	private DateWithGranularity endTime;

	private StatementRank statementRank;

	private Prefix prefix;

	public LiteralRelation() {
		super(null, null);
	}

	public LiteralRelation(Entity subject, String object, String languageCode, Prefix prefix, String property,
			DateWithGranularity startTime, DateWithGranularity endTime, DataSet dataSet, LiteralDataType dataType) {
		super(subject, dataSet);
		this.object = object;
		this.prefix = prefix;
		this.languageCode = languageCode;
		this.property = property;
		this.startTime = startTime;
		this.endTime = endTime;
		this.dataType = dataType;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public Set<SubProperty> getProperties() {
		return properties;
	}

	public void setProperties(Set<SubProperty> properties) {
		this.properties = properties;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public LiteralDataType getDataType() {
		return dataType;
	}

	public void setDataType(LiteralDataType dataType) {
		this.dataType = dataType;
	}

	public DateWithGranularity getStartTime() {
		return startTime;
	}

	public void setStartTime(DateWithGranularity startTime) {
		this.startTime = startTime;
	}

	public DateWithGranularity getEndTime() {
		return endTime;
	}

	public void setEndTime(DateWithGranularity endTime) {
		this.endTime = endTime;
	}

	public StatementRank getStatementRank() {
		return statementRank;
	}

	public void setStatementRank(StatementRank statementRank) {
		this.statementRank = statementRank;
	}

	public Prefix getPrefix() {
		return prefix;
	}

	public void setPrefix(Prefix prefix) {
		this.prefix = prefix;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

}
