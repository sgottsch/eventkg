package de.l3s.eventkg.pipeline;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public abstract class Extractor {

	private String name;

	private Source source;

	private String description;

	protected List<Language> languages;

	public Extractor(String name, Source source, String description, List<Language> languages) {
		super();
		this.name = name;
		this.source = source;
		this.description = description;
		this.languages = languages;
	}

	public abstract void run();

	public String getName() {
		return name;
	}

	public Source getSource() {
		return source;
	}

	public String getDescription() {
		return description;
	}

	public List<Language> getLanguages() {
		return languages;
	}

	public void printInformation() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(
				getName() + ", " + getSource() + " - " + getDescription() + " (" + dateFormat.format(new Date()) + ")");
	}

}
