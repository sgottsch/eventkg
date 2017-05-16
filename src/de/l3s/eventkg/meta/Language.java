package de.l3s.eventkg.meta;

public enum Language {
	EN("en"),
	DE("de"),
	ES("es"),
	RU("ru"),
	CS("cs"),
	FR("fr"),
	IT("it"),
	PL("pl"),
	PT("pt"),
	NL("nl"),
	SE("se"),
	DA("da"),
	HU("hu"),
	TR("tr");

	private String lang;

	// Constructor
	Language(String l) {
		lang = l;
	}

	// Overloaded constructor
	Language() {
		lang = null;
	}

	public String getLanguage() {
		return this.lang;
	}

	public String toString() {
		return this.lang;
	}

	public static Language getLanguage(String langStr) {
		return Language.valueOf(langStr.toUpperCase());
	}

	public String getLanguageAdjective() {
		return Language.getLanguageAdjective(this);
	}

	public static Language getLanguageByAdjective(String adjective) {

		for (Language language : Language.values()) {
			if (language.getLanguageAdjective().equals(adjective)) {
				return language;
			}
		}

		return null;
	}

	public static String getLanguageAdjective(Language language) {
		switch (language) {
		case EN: {
			return "English";
		}
		case DE: {
			return "German";
		}
		case ES: {
			return "Spanish";
		}
		case RU: {
			return "Russian";
		}
		case CS: {
			return "Czech";
		}
		case FR: {
			return "French";
		}
		case IT: {
			return "Italian";
		}
		case PL: {
			return "Polish";
		}
		case PT: {
			return "Portuguese";
		}
		case NL: {
			return "Dutch";
		}
		case SE: {
			return "Swedish";
		}
		case DA: {
			return "Danish";
		}
		case HU: {
			return "Hungarian";
		}
		case TR: {
			return "Turkish";
		}
		}
		return "";
	}

	public String getWiki() {
		return getLanguageLowerCase() + "wiki";
	}

	public String getLanguageLowerCase() {
		return this.getLanguage().toLowerCase();
	}

}
