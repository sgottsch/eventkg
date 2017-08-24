package de.l3s.eventkg.source.wikipedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.l3s.eventkg.meta.Language;
import edu.stanford.nlp.util.StringUtils;

public class WikiWords {

	private static WikiWords instance;

	private Map<Language, Set<String>> forbiddenLinks;
	private Map<Language, Set<String>> forbiddenInternalLinks;

	private Map<Language, Set<String>> fileLabels;
	private Map<Language, String> categoryLabels;
	private HashMap<Language, Set<String>> imageLabels;
	private HashMap<Language, Set<String>> listPrefixes;
	private HashMap<Language, Set<String>> categoryPrefixes;
	private Map<Language, Set<String>> eventsLabels;

	private Map<Language, List<Set<String>>> monthNames;
	private Map<Language, List<Set<String>>> weekdayNames;
	private Map<Language, String> monthRegex;
	private Map<Language, String> weekdayRegex;

	private Map<Language, Set<Pattern>> eventCategoryRegexes;

	public static WikiWords getInstance() {
		if (instance == null) {
			instance = new WikiWords();
		}
		return instance;
	}

	private WikiWords() {
	}

	public Set<String> getForbiddenInternalLinks(Language language) {
		if (this.forbiddenInternalLinks == null) {
			this.forbiddenInternalLinks = new HashMap<Language, Set<String>>();
			this.forbiddenInternalLinks.put(Language.EN, this.initForbiddenInternalLinks(Language.EN));
			this.forbiddenInternalLinks.put(Language.DE, this.initForbiddenInternalLinks(Language.DE));
			this.forbiddenInternalLinks.put(Language.PT, this.initForbiddenInternalLinks(Language.PT));
			this.forbiddenInternalLinks.put(Language.NL, this.initForbiddenInternalLinks(Language.NL));
			this.forbiddenInternalLinks.put(Language.RU, this.initForbiddenInternalLinks(Language.RU));
			this.forbiddenInternalLinks.put(Language.FR, this.initForbiddenInternalLinks(Language.FR));
			this.forbiddenInternalLinks.put(Language.IT, this.initForbiddenInternalLinks(Language.IT));
			this.forbiddenInternalLinks.put(Language.ES, this.initForbiddenInternalLinks(Language.ES));
		}
		return this.forbiddenInternalLinks.get(language);
	}

	private Set<String> initForbiddenInternalLinks(Language language) {

		Set<String> forbiddenNameSpaces = new HashSet<String>();
		Set<String> forbiddenInternalLinks = new HashSet<String>();

		String talkPrefix = null;
		String talkPrefix2 = null;
		String talkSuffix = null;
		String talkSuffix2 = null;

		if (language == Language.DE) {
			forbiddenNameSpaces.add("Diskussion");
			forbiddenNameSpaces.add("Benutzer");
			forbiddenNameSpaces.add("Benutzerin");
			forbiddenNameSpaces.add("Wikipedia");
			forbiddenNameSpaces.add("Datei");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Vorlage");
			forbiddenNameSpaces.add("Hilfe");
			forbiddenNameSpaces.add("Kategorie");
			forbiddenNameSpaces.add("Portal");
			forbiddenNameSpaces.add("Modul");
			forbiddenNameSpaces.add("Spezial");
			forbiddenNameSpaces.add("Medium");
			talkSuffix = "Diskussion";
			forbiddenInternalLinks.add("WP:");
			forbiddenInternalLinks.add("H:");
			forbiddenInternalLinks.add("P:");
			forbiddenInternalLinks.add("WD:");
			forbiddenInternalLinks.add("HD:");
			forbiddenInternalLinks.add("PD:");
		} else if (language == Language.NL) {
			forbiddenNameSpaces.add("Gebruiker");
			forbiddenNameSpaces.add("Wikipedia");
			forbiddenNameSpaces.add("Help");
			forbiddenNameSpaces.add("Bestand");
			forbiddenNameSpaces.add("Categorie");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Overleg");
			forbiddenNameSpaces.add("Sjabloon");
			forbiddenNameSpaces.add("Speciaal");
			forbiddenNameSpaces.add("Portaal");
			talkPrefix = "Overleg";
			forbiddenInternalLinks.add("WP:");
			forbiddenInternalLinks.add("H:");
		} else if (language == Language.PT) {
			forbiddenNameSpaces.add("Media");
			forbiddenNameSpaces.add("Especial");
			forbiddenNameSpaces.add("Discuss\u00e3o");
			forbiddenNameSpaces.add("Usu\u00e1rio");
			forbiddenNameSpaces.add("Wikip\u00e9dia");
			forbiddenNameSpaces.add("Ficheiro");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Predefini\u00e7\u00e3o");
			forbiddenNameSpaces.add("Ajuda");
			forbiddenNameSpaces.add("Categoria");
			forbiddenNameSpaces.add("Portal");
			forbiddenNameSpaces.add("Anexo");
			forbiddenNameSpaces.add("Imagem");
			forbiddenNameSpaces.add("Utilizador");
			talkSuffix = "Discuss\u00e3o";
			forbiddenInternalLinks.add("WP:");
			forbiddenInternalLinks.add("Discuss\u00e3o Portal:");

			// TODO: Check whether to use Discuss\u00e3o or Discussão as
			// talkSuffix
			talkSuffix2 = "Discussão";
			forbiddenNameSpaces.add("Usuário");
			forbiddenNameSpaces.add("Wikipédia");
			forbiddenNameSpaces.add("Predefinição");
			forbiddenNameSpaces.add("Discussão");
			forbiddenNameSpaces.add("Discussão Portal");

		} else if (language == Language.RU) {
			forbiddenNameSpaces.add("\u041e\u0431\u0441\u0443\u0436\u0434\u0435\u043d\u0438\u0435");
			forbiddenNameSpaces.add("\u0423\u0447\u0430\u0441\u0442\u043d\u0438\u043a");
			forbiddenNameSpaces.add("\u0412\u0438\u043a\u0438\u043f\u0435\u0434\u0438\u044f");
			forbiddenNameSpaces.add("\u0424\u0430\u0439\u043b");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("\u0428\u0430\u0431\u043b\u043e\u043d");
			forbiddenNameSpaces.add("\u0421\u043f\u0440\u0430\u0432\u043a\u0430");
			forbiddenNameSpaces.add("\u041a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u044f");
			forbiddenNameSpaces.add("\u041f\u043e\u0440\u0442\u0430\u043b");
			forbiddenNameSpaces.add("\u0418\u043d\u043a\u0443\u0431\u0430\u0442\u043e\u0440");
			forbiddenNameSpaces.add("\u0418\u043d\u043a\u0443\u0431\u0430\u0442\u043e\u0440");
			forbiddenNameSpaces.add("\u041f\u0440\u043e\u0435\u043a\u0442");
			forbiddenNameSpaces.add("\u0410\u0440\u0431\u0438\u0442\u0440\u0430\u0436");
			forbiddenNameSpaces.add(
					"\u041e\u0431\u0440\u0430\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430");
			forbiddenNameSpaces.add("\u041c\u043e\u0434\u0443\u043b\u044c");
			talkPrefix = "\u041e\u0431\u0441\u0443\u0436\u0434\u0435\u043d\u0438\u0435";
			forbiddenInternalLinks.add("WP:");

			forbiddenNameSpaces.add("Участник");
			forbiddenNameSpaces.add("Википедия");
			forbiddenNameSpaces.add("Файл");
			forbiddenNameSpaces.add("Шаблон");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Справка");
			forbiddenNameSpaces.add("Категория");
			forbiddenNameSpaces.add("Портал");
			forbiddenNameSpaces.add("Инкубатор");
			forbiddenNameSpaces.add("Проект");
			forbiddenNameSpaces.add("Арбитраж");
			forbiddenNameSpaces.add("Образовательная программа");
			forbiddenNameSpaces.add("Модуль");
			forbiddenNameSpaces.add("Обсуждение");
			talkPrefix2 = "Обсуждение";

		} else if (language == Language.IT) {
			forbiddenNameSpaces.add("File");
			forbiddenNameSpaces.add("Wikipedia");
			forbiddenNameSpaces.add("Utente");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Speciale");
			forbiddenNameSpaces.add("Media");
			forbiddenNameSpaces.add("Aiuto");
			forbiddenNameSpaces.add("Categoria");
			forbiddenNameSpaces.add("Portale");
			forbiddenNameSpaces.add("Progetto");
			forbiddenNameSpaces.add("Modulo");
			talkPrefix = "Discussioni";
			forbiddenInternalLinks.add("WP:");
		} else if (language == Language.ES) {
			forbiddenNameSpaces.add("Usuario");
			forbiddenNameSpaces.add("Wikipedia");
			forbiddenNameSpaces.add("Archivo");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Plantilla");
			forbiddenNameSpaces.add("Ayuda");
			forbiddenNameSpaces.add("Categor\u00eda");
			forbiddenNameSpaces.add("Portal");
			forbiddenNameSpaces.add("Wikiproyecto");
			forbiddenNameSpaces.add("Anexo");
			forbiddenNameSpaces.add("Programa educativo");
			forbiddenNameSpaces.add("M\u00f3dulo");
			talkSuffix = "Discusi\u00f3n";

			forbiddenNameSpaces.add("Categoría");
			forbiddenNameSpaces.add("Módulo");

			forbiddenInternalLinks.add("WP:");
		} else if (language == Language.FR) {
			forbiddenNameSpaces.add("Utilisateur");
			forbiddenNameSpaces.add("Mod\u00e8le");
			forbiddenNameSpaces.add("Projet");
			forbiddenNameSpaces.add("Wikip\u00e9dia");
			forbiddenNameSpaces.add("Aide");
			forbiddenNameSpaces.add("R\u00e9f\u00e9rence");
			forbiddenNameSpaces.add("Fichier");
			forbiddenNameSpaces.add("Cat\u00e9gorie");
			forbiddenNameSpaces.add("Module");
			talkPrefix = "Discussion";
			forbiddenInternalLinks.add("WT:");
			forbiddenInternalLinks.add("H:");
			forbiddenInternalLinks.add("CAT:");

			forbiddenNameSpaces.add("Wikipédia");
			forbiddenNameSpaces.add("Catégorie");
			forbiddenNameSpaces.add("Modèle");
			forbiddenNameSpaces.add("Référence");
		} else if (language == Language.EN) {
			forbiddenNameSpaces.add("Talk");
			forbiddenNameSpaces.add("User");
			forbiddenNameSpaces.add("Wikipedia");
			forbiddenNameSpaces.add("WP");
			forbiddenNameSpaces.add("File");
			forbiddenNameSpaces.add("MediaWiki");
			forbiddenNameSpaces.add("Template");
			forbiddenNameSpaces.add("Help");
			forbiddenNameSpaces.add("Category");
			forbiddenNameSpaces.add("Portal");
			forbiddenNameSpaces.add("Draft");
			forbiddenNameSpaces.add("Education Program");
			forbiddenNameSpaces.add("TimedText");
			forbiddenNameSpaces.add("Module");
			forbiddenNameSpaces.add("Topic");
			forbiddenNameSpaces.add("Special");
			forbiddenNameSpaces.add("Media");
			forbiddenNameSpaces.add("Image");
			talkSuffix = "talk";
			forbiddenInternalLinks.add("WT:");
			forbiddenInternalLinks.add("H:");
			forbiddenInternalLinks.add("CAT:");
		}
		for (String forbiddenNameSpace : forbiddenNameSpaces) {
			forbiddenInternalLinks.add(String.valueOf(forbiddenNameSpace) + ":");

			if (talkSuffix != null) {
				forbiddenInternalLinks.add(String.valueOf(forbiddenNameSpace) + " " + talkSuffix + ":");
				continue;
			}

			if (talkSuffix2 != null) {
				forbiddenInternalLinks.add(String.valueOf(forbiddenNameSpace) + " " + talkSuffix2 + ":");
				continue;
			}

			if (talkPrefix != null) {
				forbiddenInternalLinks.add(String.valueOf(talkPrefix) + " " + forbiddenNameSpace + ":");
				forbiddenInternalLinks.add(String.valueOf(talkPrefix) + " "
						+ forbiddenNameSpace.substring(0, 1).toLowerCase() + forbiddenNameSpace.substring(1) + ":");
			}

			if (talkPrefix2 != null) {
				forbiddenInternalLinks.add(String.valueOf(talkPrefix2) + " " + forbiddenNameSpace + ":");
				forbiddenInternalLinks.add(String.valueOf(talkPrefix2) + " "
						+ forbiddenNameSpace.substring(0, 1).toLowerCase() + forbiddenNameSpace.substring(1) + ":");
			}

		}
		return forbiddenInternalLinks;
	}

	public Set<String> getForbiddenLinks(Language language) {
		if (this.forbiddenLinks == null) {
			this.forbiddenLinks = new HashMap<Language, Set<String>>();
			this.forbiddenLinks.put(Language.EN, new HashSet<String>());
			this.forbiddenLinks.get(Language.EN).add("Wikipedia:Citation_needed");
			this.forbiddenLinks.put(Language.DE, new HashSet<String>());
			this.forbiddenLinks.get(Language.DE).add("Viertelgeviertstrich");
			this.forbiddenLinks.put(Language.PT, new HashSet<String>());
			this.forbiddenLinks.put(Language.NL, new HashSet<String>());
			this.forbiddenLinks.put(Language.RU, new HashSet<String>());
			this.forbiddenLinks.put(Language.FR, new HashSet<String>());
			this.forbiddenLinks.put(Language.IT, new HashSet<String>());
			this.forbiddenLinks.put(Language.ES, new HashSet<String>());
		}
		return this.forbiddenLinks.get(language);
	}

	public static Set<String> getTitlesOfParagraphsNotToTranslate(Language language) {
		HashSet<String> titlesNotToTranslate = new HashSet<String>();
		if (language == Language.DE) {
			titlesNotToTranslate.add("Ver\u00f6ffentlichungen");
			titlesNotToTranslate.add("Literatur");
			titlesNotToTranslate.add("Weblinks");
			titlesNotToTranslate.add("Einzelnachweise");
			titlesNotToTranslate.add("Werke");
			titlesNotToTranslate.add("Werke (Auswahl)");
		} else if (language == Language.NL) {
			titlesNotToTranslate.add("Zie ook");
			titlesNotToTranslate.add("Externe links");
			titlesNotToTranslate.add("Literatuur");
			titlesNotToTranslate.add("Bronnen, noten en referenties");
		} else if (language == Language.PT) {
			titlesNotToTranslate.add("Ver tamb\u00e9m");
			titlesNotToTranslate.add("Refer\u00eancias");
			titlesNotToTranslate.add("Liga\u00e7\u00f5es externas");
			titlesNotToTranslate.add("Notas");
			titlesNotToTranslate.add("Bibliografia");
		} else if (language == Language.EN) {
			titlesNotToTranslate.add("See also");
			titlesNotToTranslate.add("References");
			titlesNotToTranslate.add("External links");
			titlesNotToTranslate.add("Further reading");
			titlesNotToTranslate.add("Bibliography");
		} else if (language == Language.RU) {
			titlesNotToTranslate.add("\u0421\u043c. \u0442\u0430\u043a\u0436\u0435");
			titlesNotToTranslate.add("\u041f\u0440\u0438\u043c\u0435\u0447\u0430\u043d\u0438\u044f");
			titlesNotToTranslate.add("\u0412\u043d\u0435\u0448\u043d\u0438\u0435 \u0441\u0441\u044b\u043b\u043a\u0438");
			titlesNotToTranslate.add("\u0421\u0441\u044b\u043b\u043a\u0438");
			titlesNotToTranslate.add("\u0413\u0430\u043b\u0435\u0440\u0435\u044f");
		} else if (language == Language.FR) {
			titlesNotToTranslate.add("Voir aussi");
			titlesNotToTranslate.add("Annexes");
			titlesNotToTranslate.add("Notes et r\u00e9f\u00e9rences");
			titlesNotToTranslate.add("R\u00e9f\u00e9rences");
			titlesNotToTranslate.add("Compl\u00e9ments");
		} else if (language == Language.IT) {
			titlesNotToTranslate.add("Bibliografia");
			titlesNotToTranslate.add("Altri progetti");
			titlesNotToTranslate.add("Documentari");
			titlesNotToTranslate.add("Note");
			titlesNotToTranslate.add("Collegamenti esterni");
			titlesNotToTranslate.add("Voci correlate");
		} else if (language == Language.ES) {
			titlesNotToTranslate.add("V\u00e9ase tambi\u00e9n");
			titlesNotToTranslate.add("Referencias");
			titlesNotToTranslate.add("Bibliograf\u00eda");
			titlesNotToTranslate.add("Enlaces externos");
		}
		return titlesNotToTranslate;
	}

	public static String getTOCName(Language language) {
		System.out.println(language);
		if (language == Language.EN)
			return "Contents";
		else if (language == Language.DE)
			return "Inhaltsverzeichnis";
		else if (language == Language.NL)
			return "Inhoud";
		else if (language == Language.PT)
			return "\u00cdndice";
		else if (language == Language.RU)
			return "\u0421\u043e\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u0435";
		else if (language == Language.FR)
			return "Sommaire";
		else if (language == Language.ES)
			return "\u00cdndice";
		else if (language == Language.IT)
			return "Indice";

		throw new NullPointerException("No name for table of contents in that language.");
	}

	public static Set<String> getSeeAlsoLinkClasses(Language language) {
		HashSet<String> seeAlsoLinkClasses = new HashSet<String>();
		if (language == Language.DE) {
			seeAlsoLinkClasses.add("sieheauch");
			seeAlsoLinkClasses.add("hauptartikel");
		} else if (language == Language.EN) {
			// seeAlsoLinkClasses.add("seealso");
			// seeAlsoLinkClasses.add("mainarticle");
			seeAlsoLinkClasses.add("hatnote");
		} else if (language == Language.FR) {
			seeAlsoLinkClasses.add("bandeau-niveau-detail");
		} else if (language == Language.NL) {
			// won't work (is in "noprint" table structure)
		} else if (language == Language.PT) {
			seeAlsoLinkClasses.add("dablink");
		} else if (language == Language.RU) {
			seeAlsoLinkClasses.add("dablink");
		} else if (language != Language.IT && language == Language.ES) {
			seeAlsoLinkClasses.add("rellink");
		}

		return seeAlsoLinkClasses;
	}

	public static Set<String> getForbiddenImages() {
		Set<String> forbiddenImages = new HashSet<String>();

		// Source: http://wikimediafoundation.org/wiki/Wikimedia_trademarks

		forbiddenImages.add("Commons-logo.svg");
		forbiddenImages.add("WiktionaryEn.svg");
		forbiddenImages.add("Wiktionary-logo-en.svg");
		forbiddenImages.add("Wikiquote-logo.svg");
		forbiddenImages.add("WiktionaryEn.svg");
		forbiddenImages.add("Wikiquote-logo-en.svg");
		forbiddenImages.add("Wikibooks-logo.svg");
		forbiddenImages.add("Wikibooks-logo-en-noslogan.svg");
		forbiddenImages.add("Wikisource-logo.svg");
		forbiddenImages.add("Wikisource-newberg-de.png");
		forbiddenImages.add("Wikinews-logo.svg");
		forbiddenImages.add("WikiNews-Logo-en.svg");
		forbiddenImages.add("Wikiversity-logo.svg");
		forbiddenImages.add("Wikiversity-logo-en.svg");
		forbiddenImages.add("Wikispecies-logo.svg");
		forbiddenImages.add("WikiSpecies.svg");
		forbiddenImages.add("MediaWiki-notext.svg");
		forbiddenImages.add("MediaWiki.svg");
		forbiddenImages.add("Commons-logo.svg");
		forbiddenImages.add("Commons-logo-en.svg");
		forbiddenImages.add("Wikidata-logo.svg");
		forbiddenImages.add("Wikidata-logo-en.svg");
		forbiddenImages.add("Wikivoyage-Logo-v3-icon.svg");
		forbiddenImages.add("Wikivoyage-Logo-v3-en.svg");
		forbiddenImages.add("Incubator-notext");
		forbiddenImages.add("Incubator-text.svg");
		forbiddenImages.add("Wikimedia_labs_logo.svg");
		forbiddenImages.add("Wikimedia_labs_logo_with_text.svg");
		forbiddenImages.add("Wikimedia-logo.svg");
		forbiddenImages.add("Wmf_logo_vert_pms.svg");
		forbiddenImages.add("Wikimania.svg");
		forbiddenImages.add("Wikimania_logo_with_text_2.svg");

		// Others
		// TODO: Continue...

		forbiddenImages.add("Ambox_important.svg");
		forbiddenImages.add("Question_book.svg");
		forbiddenImages.add("Portal_icon.svg");

		return forbiddenImages;
	}

	public static String getWikiImageUrlBegin() {
		return "//upload.wikimedia.org";
	}

	public Set<String> getFileLabel(Language language) {
		if (this.fileLabels == null) {
			this.fileLabels = new HashMap<Language, Set<String>>();

			this.fileLabels.put(Language.EN, new HashSet<String>());
			this.fileLabels.get(Language.EN).add("File");

			this.fileLabels.put(Language.DE, new HashSet<String>());
			this.fileLabels.get(Language.DE).add("Datei");
			this.fileLabels.get(Language.DE).add("File");

			this.fileLabels.put(Language.PT, new HashSet<String>());
			this.fileLabels.get(Language.PT).add("Ficheiro");

			this.fileLabels.put(Language.RU, new HashSet<String>());
			this.fileLabels.get(Language.RU).add("Файл");

			this.fileLabels.put(Language.FR, new HashSet<String>());
			this.fileLabels.get(Language.FR).add("Fichier");
		}

		return fileLabels.get(language);
	}

	public String getCategoryLabel(Language language) {

		if (this.categoryLabels == null) {
			this.categoryLabels = new HashMap<Language, String>();
			this.categoryLabels.put(Language.EN, "Category");
			this.categoryLabels.put(Language.DE, "Kategorie");
			this.categoryLabels.put(Language.PT, "Categoria");
			this.categoryLabels.put(Language.RU, "Категория");
			this.categoryLabels.put(Language.FR, "Catégorie");
		}

		return categoryLabels.get(language);
	}

	public Set<String> getEventsLabels(Language language) {
		if (this.eventsLabels == null) {
			this.eventsLabels = new HashMap<Language, Set<String>>();
			this.eventsLabels.put(Language.EN, new HashSet<String>());
			this.eventsLabels.put(Language.DE, new HashSet<String>());
			this.eventsLabels.put(Language.FR, new HashSet<String>());
			this.eventsLabels.put(Language.PT, new HashSet<String>());
			this.eventsLabels.put(Language.RU, new HashSet<String>());

			this.eventsLabels.get(Language.EN).add("Events");
			this.eventsLabels.get(Language.DE).add("Ereignisse");
			this.eventsLabels.get(Language.FR).add("Événements");
			this.eventsLabels.get(Language.FR).add("Évènements");
			this.eventsLabels.get(Language.PT).add("Eventos");
			this.eventsLabels.get(Language.RU).add("События");
		}

		return eventsLabels.get(language);
	}

	public Set<String> getImageLabels(Language language) {
		if (this.imageLabels == null) {
			this.imageLabels = new HashMap<Language, Set<String>>();

			this.imageLabels.put(Language.EN, new HashSet<String>());
			this.imageLabels.get(Language.EN).add("Image");
			this.imageLabels.get(Language.EN).add("File");

			this.imageLabels.put(Language.DE, new HashSet<String>());
			// https://de.wikipedia.org/wiki/Hilfe:Bilder
			// "Der erste Parameter enthält den Namensraum Datei: und dann den
			// Dateinamen des Bildes. Bild: anstatt Datei: ist veraltet, wird
			// aber von der MediaWiki-Software ebenso wie die englischen
			// Begriffe File: und Image: erkannt."
			this.imageLabels.get(Language.DE).add("Datei");
			this.imageLabels.get(Language.DE).add("File");
			this.imageLabels.get(Language.DE).add("Bild");
			this.imageLabels.get(Language.DE).add("Image");

			this.imageLabels.put(Language.PT, new HashSet<String>());
			this.imageLabels.get(Language.PT).add("Imagem");
			this.imageLabels.get(Language.PT).add("Ficheiro");
			this.imageLabels.get(Language.PT).add("Arquivo");
			this.imageLabels.get(Language.PT).add("File");
			this.imageLabels.get(Language.PT).add("Image");

			this.imageLabels.put(Language.RU, new HashSet<String>());
			this.imageLabels.get(Language.RU).add("Файл");
			this.imageLabels.get(Language.RU).add("File");
			this.imageLabels.get(Language.RU).add("Image");

			this.imageLabels.put(Language.FR, new HashSet<String>());
			this.imageLabels.get(Language.FR).add("Fichier");
			this.imageLabels.get(Language.FR).add("File");
			this.imageLabels.get(Language.FR).add("Image");
		}

		return imageLabels.get(language);
	}

	// public List<String> getMonthNames(Language language) {
	//
	// if (this.monthNames == null)
	// this.monthNames = new HashMap<Language, List<String>>();
	//
	// if (this.monthNames.containsKey(language))
	// return this.monthNames.get(language);
	//
	// List<String> monthNamesOfLanguage = new ArrayList<String>();
	// this.monthNames.put(language, monthNamesOfLanguage);
	//
	// if (language == Language.EN) {
	// new ArrayList<String>();
	// monthNamesOfLanguage.add("January");
	// monthNamesOfLanguage.add("February");
	// monthNamesOfLanguage.add("March");
	// monthNamesOfLanguage.add("April");
	// monthNamesOfLanguage.add("May");
	// monthNamesOfLanguage.add("June");
	// monthNamesOfLanguage.add("July");
	// monthNamesOfLanguage.add("August");
	// monthNamesOfLanguage.add("September");
	// monthNamesOfLanguage.add("October");
	// monthNamesOfLanguage.add("November");
	// monthNamesOfLanguage.add("December");
	// } else if (language == Language.DE) {
	// new ArrayList<String>();
	// monthNamesOfLanguage.add("Januar");
	// monthNamesOfLanguage.add("Februar");
	// monthNamesOfLanguage.add("März");
	// monthNamesOfLanguage.add("April");
	// monthNamesOfLanguage.add("Mai");
	// monthNamesOfLanguage.add("Juni");
	// monthNamesOfLanguage.add("Juli");
	// monthNamesOfLanguage.add("August");
	// monthNamesOfLanguage.add("September");
	// monthNamesOfLanguage.add("Oktober");
	// monthNamesOfLanguage.add("November");
	// monthNamesOfLanguage.add("Dezember");
	// } else if (language == Language.RU) {
	// new ArrayList<String>();
	// monthNamesOfLanguage.add("Январь");
	// monthNamesOfLanguage.add("Февраль");
	// monthNamesOfLanguage.add("Март");
	// monthNamesOfLanguage.add("Апрель");
	// monthNamesOfLanguage.add("Май");
	// monthNamesOfLanguage.add("Июнь");
	// monthNamesOfLanguage.add("Июль");
	// monthNamesOfLanguage.add("Август");
	// monthNamesOfLanguage.add("Сентябрь");
	// monthNamesOfLanguage.add("Октябрь");
	// monthNamesOfLanguage.add("Ноябрь");
	// monthNamesOfLanguage.add("Декабрь");
	// } else if (language == Language.FR) {
	// new ArrayList<String>();
	// monthNamesOfLanguage.add("Janvier");
	// monthNamesOfLanguage.add("Février");
	// monthNamesOfLanguage.add("Mars");
	// monthNamesOfLanguage.add("Avril");
	// monthNamesOfLanguage.add("Mai");
	// monthNamesOfLanguage.add("Juin");
	// monthNamesOfLanguage.add("Juillet");
	// monthNamesOfLanguage.add("Août");
	// monthNamesOfLanguage.add("Septembre");
	// monthNamesOfLanguage.add("Octobre");
	// monthNamesOfLanguage.add("Novembre");
	// monthNamesOfLanguage.add("Décembre");
	// } else if (language == Language.PT) {
	// new ArrayList<String>();
	// monthNamesOfLanguage.add("Janeiro");
	// monthNamesOfLanguage.add("Fevereiro");
	// monthNamesOfLanguage.add("Março");
	// monthNamesOfLanguage.add("Abril");
	// monthNamesOfLanguage.add("Maio");
	// monthNamesOfLanguage.add("Junho");
	// monthNamesOfLanguage.add("Julho");
	// monthNamesOfLanguage.add("Agosto");
	// monthNamesOfLanguage.add("Setembro");
	// monthNamesOfLanguage.add("Outubro");
	// monthNamesOfLanguage.add("Novembro");
	// monthNamesOfLanguage.add("Dezembro");
	// } else {
	// System.out.println("Month names: Language not supported.");
	// }
	//
	// return monthNamesOfLanguage;
	// }

	public List<Set<String>> getMonthNames(Language language) {

		if (this.monthNames == null)
			this.monthNames = new HashMap<Language, List<Set<String>>>();

		if (this.monthNames.containsKey(language))
			return this.monthNames.get(language);

		List<Set<String>> monthNamesOfLanguage = new ArrayList<Set<String>>();
		this.monthNames.put(language, monthNamesOfLanguage);

		List<String> monthNamesSeparated = getMonthNamesSemicolonSeparated(language);

		for (int i = 0; i < 12; i++) {
			Set<String> monthNameOptions = new HashSet<String>();
			for (String name : monthNamesSeparated.get(i).split(";"))
				monthNameOptions.add(name);
			monthNamesOfLanguage.add(monthNameOptions);
		}

		return monthNamesOfLanguage;
	}

	public List<Set<String>> getWeekdayNames(Language language) {

		if (this.weekdayNames == null)
			this.weekdayNames = new HashMap<Language, List<Set<String>>>();

		if (this.weekdayNames.containsKey(language))
			return this.weekdayNames.get(language);

		List<Set<String>> weekdayNamesOfLanguage = new ArrayList<Set<String>>();
		this.weekdayNames.put(language, weekdayNamesOfLanguage);

		List<String> weekdayNamesSeparated = getWeekdayNamesSemicolonSeparated(language);

		for (int i = 0; i < 7; i++) {
			Set<String> weekdayNameOptions = new HashSet<String>();
			for (String name : weekdayNamesSeparated.get(i).split(";"))
				weekdayNameOptions.add(name);
			weekdayNamesOfLanguage.add(weekdayNameOptions);
		}

		return weekdayNamesOfLanguage;
	}

	public String getMonthRegex(Language language) {

		if (this.monthRegex == null)
			this.monthRegex = new HashMap<Language, String>();
		if (!this.monthRegex.containsKey(language)) {

			List<String> regexParts = new ArrayList<String>();

			for (Set<String> names : getMonthNames(language)) {
				regexParts.add("(" + StringUtils.join(names, "|") + ")");
			}

			this.monthRegex.put(language, "(" + StringUtils.join(regexParts, "|") + ")");
		}

		return this.monthRegex.get(language);
	}

	public String getWeekdayRegex(Language language) {

		if (this.weekdayRegex == null)
			this.weekdayRegex = new HashMap<Language, String>();
		if (!this.weekdayRegex.containsKey(language)) {

			List<String> regexParts = new ArrayList<String>();

			for (Set<String> names : getWeekdayNames(language)) {
				regexParts.add("(" + StringUtils.join(names, "|") + ")");
			}

			this.weekdayRegex.put(language, "(" + StringUtils.join(regexParts, "|") + ")");
		}

		return this.weekdayRegex.get(language);
	}

	private List<String> getMonthNamesSemicolonSeparated(Language language) {

		List<String> monthNamesOfLanguage = new ArrayList<String>();

		if (language == Language.EN) {
			new ArrayList<String>();
			monthNamesOfLanguage.add("January");
			monthNamesOfLanguage.add("February");
			monthNamesOfLanguage.add("March");
			monthNamesOfLanguage.add("April");
			monthNamesOfLanguage.add("May");
			monthNamesOfLanguage.add("June");
			monthNamesOfLanguage.add("July");
			monthNamesOfLanguage.add("August");
			monthNamesOfLanguage.add("September");
			monthNamesOfLanguage.add("October");
			monthNamesOfLanguage.add("November");
			monthNamesOfLanguage.add("December");
		} else if (language == Language.DE) {
			new ArrayList<String>();
			monthNamesOfLanguage.add("Januar");
			monthNamesOfLanguage.add("Februar");
			monthNamesOfLanguage.add("März");
			monthNamesOfLanguage.add("April");
			monthNamesOfLanguage.add("Mai");
			monthNamesOfLanguage.add("Juni");
			monthNamesOfLanguage.add("Juli");
			monthNamesOfLanguage.add("August");
			monthNamesOfLanguage.add("September");
			monthNamesOfLanguage.add("Oktober");
			monthNamesOfLanguage.add("November");
			monthNamesOfLanguage.add("Dezember");
		} else if (language == Language.RU) {
			new ArrayList<String>();
			monthNamesOfLanguage.add("Январь;января");
			monthNamesOfLanguage.add("Февраль;февраля");
			monthNamesOfLanguage.add("Март;марта");
			monthNamesOfLanguage.add("Апрель;апреля");
			monthNamesOfLanguage.add("Май;мая");
			monthNamesOfLanguage.add("Июнь;июня");
			monthNamesOfLanguage.add("Июль;июля");
			monthNamesOfLanguage.add("Август;августа");
			monthNamesOfLanguage.add("Сентябрь;сентября");
			monthNamesOfLanguage.add("Октябрь;октября");
			monthNamesOfLanguage.add("Ноябрь;ноября");
			monthNamesOfLanguage.add("Декабрь;декабря");
		} else if (language == Language.FR) {
			new ArrayList<String>();
			monthNamesOfLanguage.add("Janvier;janvier");
			monthNamesOfLanguage.add("Février;février");
			monthNamesOfLanguage.add("Mars;mars");
			monthNamesOfLanguage.add("Avril;avril");
			monthNamesOfLanguage.add("Mai;mai");
			monthNamesOfLanguage.add("Juin;juin");
			monthNamesOfLanguage.add("Juillet;juillet");
			monthNamesOfLanguage.add("Août;août");
			monthNamesOfLanguage.add("Septembre;septembre");
			monthNamesOfLanguage.add("Octobre;octobre");
			monthNamesOfLanguage.add("Novembre;novembre");
			monthNamesOfLanguage.add("Décembre;décembre");
		} else if (language == Language.PT) {
			new ArrayList<String>();
			monthNamesOfLanguage.add("Janeiro;janeiro");
			monthNamesOfLanguage.add("Fevereiro;fevereiro");
			monthNamesOfLanguage.add("Março;março");
			monthNamesOfLanguage.add("Abril;abril");
			monthNamesOfLanguage.add("Maio;maio");
			monthNamesOfLanguage.add("Junho;junho");
			monthNamesOfLanguage.add("Julho;julho");
			monthNamesOfLanguage.add("Agosto;agosto");
			monthNamesOfLanguage.add("Setembro;setembro");
			monthNamesOfLanguage.add("Outubro;outubro");
			monthNamesOfLanguage.add("Novembro;novembro");
			monthNamesOfLanguage.add("Dezembro;dezembro");
		} else {
			System.out.println("Month names: Language not supported.");
		}

		return monthNamesOfLanguage;
	}

	private List<String> getWeekdayNamesSemicolonSeparated(Language language) {

		List<String> weekdayNamesOfLanguage = new ArrayList<String>();

		if (language == Language.EN) {
			new ArrayList<String>();
			weekdayNamesOfLanguage.add("Monday");
			weekdayNamesOfLanguage.add("Tuesday");
			weekdayNamesOfLanguage.add("Wednesday");
			weekdayNamesOfLanguage.add("Thursday");
			weekdayNamesOfLanguage.add("Friday");
			weekdayNamesOfLanguage.add("Saturday");
			weekdayNamesOfLanguage.add("Sunday");
		} else if (language == Language.DE) {
			new ArrayList<String>();
			weekdayNamesOfLanguage.add("Montag");
			weekdayNamesOfLanguage.add("Dienstag");
			weekdayNamesOfLanguage.add("Mittwoch");
			weekdayNamesOfLanguage.add("Donnerstag");
			weekdayNamesOfLanguage.add("Freitag");
			weekdayNamesOfLanguage.add("Samstag;Sonnabend");
			weekdayNamesOfLanguage.add("Sonntag");
		} else if (language == Language.RU) {
			new ArrayList<String>();
			weekdayNamesOfLanguage.add("понедельник");
			weekdayNamesOfLanguage.add("вторник");
			weekdayNamesOfLanguage.add("среда");
			weekdayNamesOfLanguage.add("четверг");
			weekdayNamesOfLanguage.add("пятница");
			weekdayNamesOfLanguage.add("суббота");
			weekdayNamesOfLanguage.add("воскресенье");
		} else if (language == Language.FR) {
			new ArrayList<String>();
			weekdayNamesOfLanguage.add("lundi;Lundi");
			weekdayNamesOfLanguage.add("mardi;Mardi");
			weekdayNamesOfLanguage.add("mercredi;Mercredi");
			weekdayNamesOfLanguage.add("jeudi;Jeudi");
			weekdayNamesOfLanguage.add("vendredi;Vendredi");
			weekdayNamesOfLanguage.add("samedi;Samedi");
			weekdayNamesOfLanguage.add("dimanche;Dimanche");
		} else if (language == Language.PT) {
			new ArrayList<String>();
			weekdayNamesOfLanguage.add("segunda-feira;Segunda-feira");
			weekdayNamesOfLanguage.add("terça-feira;Terça-feira");
			weekdayNamesOfLanguage.add("quarta-feira;Quarta-feira");
			weekdayNamesOfLanguage.add("quinta-feira;Quinta-feira");
			weekdayNamesOfLanguage.add("sexta-feira;Sexta-feira");
			weekdayNamesOfLanguage.add("sábado;Sábado");
			weekdayNamesOfLanguage.add("domingo;Domingo");
		} else {
			System.out.println("Month names: Language not supported.");
		}

		return weekdayNamesOfLanguage;
	}

	public Set<String> getListPrefixes(Language language) {
		if (this.listPrefixes == null) {
			this.listPrefixes = new HashMap<Language, Set<String>>();

			this.listPrefixes.put(Language.EN, new HashSet<String>());
			this.listPrefixes.get(Language.EN).add("Lists_of_");
			this.listPrefixes.get(Language.EN).add("List_of_");
			this.listPrefixes.get(Language.EN).add("Alphabetical_list_of_");
			this.listPrefixes.get(Language.EN).add("Chronological_list_of_");

			this.listPrefixes.put(Language.DE, new HashSet<String>());
			this.listPrefixes.get(Language.DE).add("Liste_");
			this.listPrefixes.get(Language.DE).add("Listen_");
			this.listPrefixes.get(Language.DE).add("Übersicht_der_Listen_");

			this.listPrefixes.put(Language.PT, new HashSet<String>());
			this.listPrefixes.get(Language.PT).add("Lista_");
			this.listPrefixes.get(Language.PT).add("Listas_");

			this.listPrefixes.put(Language.RU, new HashSet<String>());
			this.listPrefixes.get(Language.RU).add("Список_");

			this.listPrefixes.put(Language.FR, new HashSet<String>());
			this.listPrefixes.get(Language.FR).add("Liste_");
		}

		return listPrefixes.get(language);
	}

	public Set<String> getCategoryPrefixes(Language language) {
		if (this.categoryPrefixes == null) {
			this.categoryPrefixes = new HashMap<Language, Set<String>>();

			this.categoryPrefixes.put(Language.EN, new HashSet<String>());
			this.categoryPrefixes.get(Language.EN).add("Category:");

			this.categoryPrefixes.put(Language.DE, new HashSet<String>());
			this.categoryPrefixes.get(Language.DE).add("Kategorie:");

			this.categoryPrefixes.put(Language.PT, new HashSet<String>());
			this.categoryPrefixes.get(Language.PT).add("Categoria:");

			this.categoryPrefixes.put(Language.RU, new HashSet<String>());
			this.categoryPrefixes.get(Language.RU).add("Категория:");

			this.categoryPrefixes.put(Language.FR, new HashSet<String>());
			this.categoryPrefixes.get(Language.FR).add("Catégorie:");

			this.categoryPrefixes.put(Language.ES, new HashSet<String>());
			this.categoryPrefixes.get(Language.ES).add("Categoría:");
		}

		return categoryPrefixes.get(language);
	}

	public Set<Pattern> getEventCategoryRegexes(Language language) {
		if (this.eventCategoryRegexes == null) {
			this.eventCategoryRegexes = new HashMap<Language, Set<Pattern>>();

			this.eventCategoryRegexes.put(Language.EN, new HashSet<Pattern>());
			this.eventCategoryRegexes.get(Language.EN).add(Pattern.compile(".+events$"));

			this.eventCategoryRegexes.put(Language.DE, new HashSet<Pattern>());

			this.eventCategoryRegexes.put(Language.PT, new HashSet<Pattern>());

			this.eventCategoryRegexes.put(Language.RU, new HashSet<Pattern>());
			this.eventCategoryRegexes.get(Language.RU).add(Pattern.compile("^События.+$"));

			this.eventCategoryRegexes.put(Language.FR, new HashSet<Pattern>());

			this.eventCategoryRegexes.put(Language.ES, new HashSet<Pattern>());
		}

		return eventCategoryRegexes.get(language);
	}

}
