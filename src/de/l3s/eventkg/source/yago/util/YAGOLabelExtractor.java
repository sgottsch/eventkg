package de.l3s.eventkg.source.yago.util;

import java.util.List;

import org.apache.commons.lang3.EnumUtils;

import de.l3s.eventkg.meta.Language;

public class YAGOLabelExtractor {

	private String id;
	private String wikipediaLabel;
	private Language language;

	private List<Language> languages;

	private boolean isValid = true;

	public YAGOLabelExtractor(String id, List<Language> languages) {
		this.id = id;
		this.languages = languages;
	}

	public void extractLabel() {

		this.language = Language.EN;

		if (this.id.startsWith("<") && id.endsWith(">"))
			this.id = this.id.substring(1, this.id.length() - 1);

		this.wikipediaLabel = this.id.replaceAll(" ", "_");

		// YAGO labels can be like "<de/Sass_Pordoi>". Extract language
		// then.
		if (wikipediaLabel.length() > 2 && wikipediaLabel.charAt(2) == '/') {
			String languageString = wikipediaLabel.substring(0, 2);

			if (!EnumUtils.isValidEnum(Language.class, languageString.toUpperCase())) {
				this.isValid = false;
				return;
			}

			if (Language.valueOf(languageString.toUpperCase()) != null) {
				language = Language.valueOf(languageString.toUpperCase());
				if (!this.languages.contains(language)) {
					this.isValid = false;
					return;
				}
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

	public boolean isValid() {
		return isValid;
	}

}
