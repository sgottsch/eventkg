package de.l3s.eventkg.integration.model.relation;

import java.util.HashMap;
import java.util.Map;

public class LiteralDataTypeCollection {

	private static LiteralDataTypeCollection instance;

	public static LiteralDataTypeCollection getInstance() {
		if (instance == null) {
			instance = new LiteralDataTypeCollection();
			instance.init();
		}
		return instance;
	}

	private LiteralDataTypeCollection() {
	}

	private Map<String, LiteralDataType> dataTypes;

	public void init() {
		this.dataTypes = new HashMap<String, LiteralDataType>();

		dataTypes.put("<http://www.w3.org/2001/XMLSchema#date>", LiteralDataType.DATE);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#double>", LiteralDataType.DOUBLE);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#gYear>", LiteralDataType.YEAR);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#nonNegativeInteger>", LiteralDataType.NON_NEGATIVE_INTEGER);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#integer>", LiteralDataType.INTEGER);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#float>", LiteralDataType.FLOAT);
		dataTypes.put("<http://dbpedia.org/datatype/usDollar>", LiteralDataType.US_DOLLAR);
		dataTypes.put("<http://www.w3.org/2001/XMLSchema#boolean>", LiteralDataType.BOOLEAN);

		dataTypes.put("http://www.wikidata.org/entity/Q577", LiteralDataType.YEAR);
		dataTypes.put("https://www.wikidata.org/wiki/Q4917", LiteralDataType.US_DOLLAR);
		dataTypes.put("https://www.wikidata.org/wiki/Q11229", LiteralDataType.PERCENT);
		dataTypes.put("https://www.wikidata.org/wiki/Q218593", LiteralDataType.INCH);
		dataTypes.put("https://www.wikidata.org/wiki/Q11573", LiteralDataType.METERS);

		dataTypes.put("<percent>", LiteralDataType.PERCENT);
		dataTypes.put("<degrees>", LiteralDataType.DEGREES);
		dataTypes.put("xsd:integer", LiteralDataType.INTEGER);
		dataTypes.put("xsd:decimal", LiteralDataType.DECIMAL);
		dataTypes.put("</km2>", LiteralDataType.SQUARE_KM);
		dataTypes.put("<km2>", LiteralDataType.KM);
		dataTypes.put("<m>", LiteralDataType.METERS);
		dataTypes.put("<s>", LiteralDataType.SECONDS);

		dataTypes.put("", LiteralDataType.LANG_STRING);
	}

	public LiteralDataType getDataType(String dataTypeString) {
		return dataTypes.get(dataTypeString);
	}

}
