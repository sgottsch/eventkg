package de.l3s.eventkg.integration.model.relation;

public enum Prefix {

	SCHEMA_ORG("so:", "http:schema.org/"),
	RDF("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
	RDFS("rdfs:", "http://www.w3.org/2000/01/rdf-schema#"),
	DCTERMS("dcterms:", "http://purl.org/dc/terms/"),
	XSD("xsd:", "http://www.w3.org/2001/XMLSchema#"),
	EVENT_KG("eventKG:", "http://eventKG.l3s.uni-hannover.de/schema/"),
	VOID("void:", "http://rdfs.org/ns/void#"),
	WIKIDATA("wikidata:", "https://www.wikidata.org/wiki/"),
	FOAF("foaf:", "http://xmlns.com/foaf/0.1/"),
	DBPEDIA_DE("dbpedia-de:", "http://es.dbpedia.org/page/"),
	DBPEDIA_EN("dbpedia-en:", "http://dbpedia.org/page/"),
	DBPEDIA_PT("dbpedia-pt:", "http://pt.dbpedia.org/page/"),
	DBPEDIA_FR("dbpedia-fr:", "http://fr.dbpedia.org/page/"),
	DBPEDIA_RU("dbpedia-ru:", "http://ru.dbpedia.org/page/");
	// TODO: Solve that better...

	private String urlPrefix;
	private String abbr;

	Prefix(String abbr, String urlPrefix) {
		this.abbr = abbr;
		this.urlPrefix = urlPrefix;
	}

	public String getUrlPrefix() {
		return urlPrefix;
	}

	public String getAbbr() {
		return abbr;
	}

}
