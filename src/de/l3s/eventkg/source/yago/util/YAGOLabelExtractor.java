package de.l3s.eventkg.source.yago.util;

import de.l3s.eventkg.meta.Language;

public class YAGOLabelExtractor {

	private String id;
	private String wikipediaLabel;
	private Language language;

	public YAGOLabelExtractor(String id) {
		this.id = id;
	}

	public void extractLabel() {

		this.language = Language.EN;

		if (this.id.startsWith("<") && id.endsWith(">"))
			this.id = this.id.substring(1, this.id.length() - 1);

		this.wikipediaLabel = this.id.replaceAll(" ", "_");

		// YAGO labels can be like "<de/Sass_Pordoi>". Extract language
		// then.
		if (wikipediaLabel.charAt(2) == '/') {
			String languageString = wikipediaLabel.substring(0, 2);
			if (Language.valueOf(languageString.toUpperCase()) != null) {
				language = Language.valueOf(languageString.toUpperCase());
				wikipediaLabel = wikipediaLabel.substring(3);
			}
		}
	}

	public String getWikipediaLabel() {
		return wikipediaLabel;
	}

	public Language getLanguage() {
		return language;
	}

}
