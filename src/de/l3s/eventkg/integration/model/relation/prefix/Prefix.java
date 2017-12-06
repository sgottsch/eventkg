package de.l3s.eventkg.integration.model.relation.prefix;

public class Prefix {

	private String urlPrefix;
	private String abbr;

	private PrefixEnum prefixEnum;

	public Prefix(String urlPrefix, String abbr, PrefixEnum prefixEnum) {
		super();
		this.urlPrefix = urlPrefix;
		this.abbr = abbr;
		this.prefixEnum = prefixEnum;
	}

	public String getUrlPrefix() {
		return urlPrefix;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public String getAbbr() {
		return abbr;
	}

	public void setAbbr(String abbr) {
		this.abbr = abbr;
	}

	public PrefixEnum getPrefixEnum() {
		return prefixEnum;
	}

}
