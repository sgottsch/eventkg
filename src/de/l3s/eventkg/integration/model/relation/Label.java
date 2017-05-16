package de.l3s.eventkg.integration.model.relation;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class Label extends RelationWithSource {

	private String label;

	private Language language;

	private Source source;

	public Label(Entity subject, DataSet dataSet, String label, Language language) {
		super(subject, dataSet);
		this.label = label;
		this.language = language;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
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
