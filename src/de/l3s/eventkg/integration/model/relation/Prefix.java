package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.DataStoreWriter;

public enum Prefix {

	SCHEMA_ORG("so:", "http://schema.org/"),
	RDF("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
	RDFS("rdfs:", "http://www.w3.org/2000/01/rdf-schema#"),
	DCTERMS("dcterms:", "http://purl.org/dc/terms/"),
	XSD("xsd:", "http://www.w3.org/2001/XMLSchema#"),
	EVENT_KG_OLD("eventKG:", "http://eventKG.l3s.uni-hannover.de/schema/"),
	EVENT_KG_SCHEMA("eventKG-s:", "http://eventKG.l3s.uni-hannover.de/schema/"),
	EVENT_KG_RESOURCE("eventKG-r:", "http://eventKG.l3s.uni-hannover.de/resource/"),
	EVENT_KG_GRAPH("eventKG-g:", "http://eventKG.l3s.uni-hannover.de/graph/"),
	VOID("void:", "http://rdfs.org/ns/void#"),
	WIKIDATA_PROPERTY("wdt:", "http://www.wikidata.org/prop/direct/"),
	WIKIDATA_ENTITY("wd:", "http://www.wikidata.org/entity/"),
	FOAF("foaf:", "http://xmlns.com/foaf/0.1/"),
	OWL("owl:", "http://www.w3.org/2002/07/owl#"),
	// TODO: Solve that better. Mapping from lang to prefix.
	DBPEDIA_RESOURCE_EN("dbr:", "http://dbpedia.org/resource/"),
	DBPEDIA_RESOURCE_DE("dbpedia-de:", "http://de.dbpedia.org/resource/"),
	DBPEDIA_RESOURCE_PT("dbpedia-pt:", "http://pt.dbpedia.org/resource/"),
	DBPEDIA_RESOURCE_FR("dbpedia-fr:", "http://fr.dbpedia.org/resource/"),
	DBPEDIA_RESOURCE_RU("dbpedia-ru:", "http://ru.dbpedia.org/resource/"),
	DBPEDIA_ONTOLOGY("dbo:", "http://dbpedia.org/ontology/"),
	// DBPEDIA_PROPERTY_DE("dbpprop-de:", "http://de.dbpedia.org/property/"),
	// DBPEDIA_PROPERTY_PT("dbpprop-pt:", "http://pt.dbpedia.org/property/"),
	// DBPEDIA_PROPERTY_FR("dbpprop-fr:", "http://fr.dbpedia.org/property/"),
	// DBPEDIA_PROPERTY_RU("dbpprop-ru:", "http://ru.dbpedia.org/property/"),
	YAGO("yago:", "http://yago-knowledge.org/resource/");

	private String urlPrefix;
	private String abbr;

	Prefix(String abbr, String urlPrefix) {
		this.abbr = abbr;
		this.urlPrefix = urlPrefix;
	}

	public String getUrlPrefix() {

		if (DataStoreWriter.OLD_MODEL) {
			if (this == EVENT_KG_SCHEMA || this == EVENT_KG_RESOURCE || this == EVENT_KG_GRAPH)
				return EVENT_KG_OLD.urlPrefix;
		}

		return urlPrefix;
	}

	public String getAbbr() {

		if (DataStoreWriter.OLD_MODEL) {
			if (this == EVENT_KG_SCHEMA || this == EVENT_KG_RESOURCE || this == EVENT_KG_GRAPH)
				return EVENT_KG_OLD.abbr;
		}

		return abbr;
	}

}
