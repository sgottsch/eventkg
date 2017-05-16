package de.l3s.eventkg.yago.model;

public class YAGOMetaFact {

	private String property;
	private String object;

	private boolean isTemporal;

	public YAGOMetaFact(String property, String object) {
		super();
		this.property = property;
		this.object = object;

		// possible properties: <hasGloss>, <byTransport>, rdfs:comment,
		// <occursSince>, <occursUntil>, <hasPredecessor>, <hasSuccessor>

		if (property.equals("<occursSince>") || property.equals("<occursUntil>"))
			isTemporal = true;
		else
			isTemporal = false;
	}

	public String getProperty() {
		return property;
	}

	public String getObject() {
		return object;
	}

	public boolean isTemporal() {
		return isTemporal;
	}

}
