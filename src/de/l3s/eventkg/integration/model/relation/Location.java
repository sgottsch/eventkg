package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class Location extends RelationWithSource {

	private Entity location;

	private Language language;

	private Source source;

	public Location(Event subject, DataSet dataSet, Entity location, Language language) {
		super(subject, dataSet);
		this.location = location;
		this.language = language;
	}

	public Entity getLocation() {
		return location;
	}

	public void setLocation(Entity location) {
		this.location = location;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
