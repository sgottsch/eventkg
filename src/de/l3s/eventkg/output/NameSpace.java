package de.l3s.eventkg.output;

public enum NameSpace {

	RDF("rdf", "http:rdf");

	private String urlPrefix;
	private String abbr;

	NameSpace(String abbr, String urlPrefix) {
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
