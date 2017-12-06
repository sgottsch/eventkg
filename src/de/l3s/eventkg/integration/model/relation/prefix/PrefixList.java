package de.l3s.eventkg.integration.model.relation.prefix;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.meta.Language;

public class PrefixList {

	private static PrefixList singleton;

	private Set<Prefix> allPrefixes = new HashSet<Prefix>();

	private Map<PrefixEnum, Prefix> prefixes = new HashMap<PrefixEnum, Prefix>();
	private Map<PrefixEnum, Map<Language, Prefix>> prefixesByLanguage = new HashMap<PrefixEnum, Map<Language, Prefix>>();

	private PrefixList() {
	}

	public static synchronized PrefixList getInstance() {
		if (singleton == null) {
			singleton = new PrefixList();
		}
		return singleton;
	}

	public void init(List<Language> languages) {

		// DBpedia prefixes are language dependent. To have a dynamic list of
		// prefixes, we need to consider that. That's why there is no simple
		// enum.

		for (PrefixEnum prefixEnum : PrefixEnum.values()) {
			if (prefixEnum.getAbbr() == null)
				continue;
			Prefix prefix = new Prefix(prefixEnum.getUrlPrefix(), prefixEnum.getAbbr(), prefixEnum);
			prefixes.put(prefixEnum, prefix);
			allPrefixes.add(prefix);
		}

		this.prefixesByLanguage.put(PrefixEnum.DBPEDIA_RESOURCE, new HashMap<Language, Prefix>());

		Prefix prefixDbpediaEn = new Prefix("dbr:", "http://dbpedia.org/resource/", PrefixEnum.DBPEDIA_RESOURCE);
		this.prefixesByLanguage.get(PrefixEnum.DBPEDIA_RESOURCE).put(Language.EN, prefixDbpediaEn);
		allPrefixes.add(prefixDbpediaEn);

		for (Language language : languages) {
			Prefix prefixDbpediaLang = new Prefix("dbpedia-" + language.getLanguage().toLowerCase() + ":",
					"http://dbpedia.org/resource/", PrefixEnum.DBPEDIA_RESOURCE);
			this.prefixesByLanguage.get(PrefixEnum.DBPEDIA_RESOURCE).put(language, prefixDbpediaLang);
			allPrefixes.add(prefixDbpediaLang);
		}
	}

	public Prefix getPrefix(PrefixEnum prefix) {
		return this.prefixes.get(prefix);
	}

	public Prefix getPrefix(PrefixEnum prefix, Language language) {
		return this.prefixesByLanguage.get(prefix).get(language);
	}

	public Set<Prefix> getAllPrefixes() {
		return allPrefixes;
	}

}
