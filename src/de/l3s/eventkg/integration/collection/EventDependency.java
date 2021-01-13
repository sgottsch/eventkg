package de.l3s.eventkg.integration.collection;

import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;

public enum EventDependency {

	SUB_EVENT(PrefixEnum.SEM, "hasSubEvent"),
	SUB_EVENT_OF(PrefixEnum.SEM, "subEventOf"),
	NEXT_EVENT(PrefixEnum.DBPEDIA_ONTOLOGY, "nextEvent"),
	PREVIOUS_EVENT(PrefixEnum.DBPEDIA_ONTOLOGY, "previousEvent");

	private PrefixEnum prefix;
	private String property;

	private EventDependency(PrefixEnum prefix, String property) {
		this.prefix = prefix;
		this.property = property;
	}

	public PrefixEnum getPrefix() {
		return prefix;
	}

	public String getProperty() {
		return property;
	}

}
