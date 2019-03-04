package de.l3s.eventkg.integration.model.relation;

public enum LiteralDataType {

	LANG_STRING("", true),
	INTEGER("xsd:integer", true),
	FLOAT("xsd:float", true),
	PERCENT("xsd:double", true),
	DECIMAL("xsd:decimal", true),
	BOOLEAN("xsd:boolean", true),
	DEGREES(null, false),
	DOUBLE("xsd:double", true),
	DATE("xsd:date", true),
	NON_NEGATIVE_INTEGER("xsd:nonNegativeInteger", true),
	METERS(null, false),
	INCH(null, false),
	SECONDS(null, false),
	KM(null, false),
	SQUARE_KM(null, false),
	YEAR("xsd:gYear", false),
	YEAR_MONTH("xsd:gYearMonth", false),
	US_DOLLAR(null, false);

	private String dataTypeRdf;

	private boolean isUsed;

	LiteralDataType(String dataTypeRdf, boolean isUsed) {
		this.dataTypeRdf = dataTypeRdf;
		this.isUsed = isUsed;
	}

	public String getDataTypeRdf() {
		return dataTypeRdf;
	}

	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

}
