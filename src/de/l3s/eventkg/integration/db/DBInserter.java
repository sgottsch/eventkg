package de.l3s.eventkg.integration.db;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Sets;
import com.sleepycat.je.Database;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikidata.WikidataResource;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.MemoryStatsUtil;
import de.l3s.eventkg.util.TimeTransformer;

public class DBInserter extends Extractor {

	private Map<Language, Set<String>> forbiddenPrefixesPerLanguage = new HashMap<Language, Set<String>>();

	private Set<Integer> validWikidataIds = new HashSet<Integer>();
	private Set<Integer> usedWikidataIds = new HashSet<Integer>();

	public DBInserter(List<Language> languages) {
		super("DBInserter", de.l3s.eventkg.meta.Source.ALL, "Create DB of entity labels.", languages);
	}

	public void run() {

		init();

		loadWikidataIdMapping();
		loadWikidataLabels();

		createFileOfValidWikidataIDs();
	}

	private void init() {

		for (Language language : this.languages) {
			Set<String> forbiddenPrefixes = new HashSet<String>();
			for (String forbiddenPrefix : WikiWords.getInstance().getListPrefixes(language)) {
				forbiddenPrefixes.add(forbiddenPrefix);
			}
			for (String forbiddenPrefix : WikiWords.getInstance().getCategoryPrefixes(language)) {
				forbiddenPrefixes.add(forbiddenPrefix);
			}
			for (String forbiddenPrefix : WikiWords.getInstance().getTemplatePrefixes(language)) {
				forbiddenPrefixes.add(forbiddenPrefix);
			}
			forbiddenPrefixesPerLanguage.put(language, forbiddenPrefixes);
		}

		this.validWikidataIds = loadAllValidWikidataEntities();
	}

	private void loadWikidataIdMapping() {

		DatabaseCreator dbCreator = new DatabaseCreator();

		MemoryStatsUtil.printMemoryStats();

		for (Language language : this.languages) {

			System.out.println("Load Wikidata mapping for the " + language.getLanguageAdjective()
					+ " Wikipedia in Wikidata - " + TimeTransformer.getTime() + ".");

			Set<String> forbiddenPrefixes = this.forbiddenPrefixesPerLanguage.get(language);

			int lines = 0;

			long ms = System.currentTimeMillis();

			Database db = dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_ID);
			Database db2 = dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID);

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.ID_TO_WIKIPEDIA_MAPPING_FILE_NAME, language);
				while (it.hasNext()) {

					// TODO: Remove
					if (lines == 1000)
						break;

					if (lines % 1000000 == 0) {
						long newMS = System.currentTimeMillis();

						System.out.println(lines + "\t" + language.getLanguage() + "\t" + TimeTransformer.getTime()
								+ " - " + (newMS - ms));
						ms = newMS;
					}

					String line = it.nextLine();

					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String wikipediaId = parts[1].replaceAll(" ", "_");

					if (!labelIsValid(wikipediaId, forbiddenPrefixes)) {
						continue;
					}

					int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

					if (!this.validWikidataIds.contains(numericWikidataId)) {
						continue;
					}

					dbCreator.createEntry(db, String.valueOf(numericWikidataId), wikipediaId);
					dbCreator.createEntry(db2, wikipediaId, String.valueOf(numericWikidataId));

					this.usedWikidataIds.add(numericWikidataId);

					lines += 1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it.close();
					// dbCreator.closeDB(db);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println(language + ": " + lines + " labels.");

			MemoryStatsUtil.printMemoryStats();

		}

	}

	private void loadWikidataLabels() {

		DatabaseCreator dbCreator = new DatabaseCreator();

		int ignoredEntities = 0;

		Set<Database> wikidataIdToWikipediaIdDBs = new HashSet<Database>();
		for (Language language : languages) {
			wikidataIdToWikipediaIdDBs.add(dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_ID));
		}

		Set<Database> wikipediaIdToWikipediaIdDBs = new HashSet<Database>();
		for (Language language : languages) {
			wikipediaIdToWikipediaIdDBs.add(dbCreator.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID));
		}

		for (Language language : this.languages) {

			System.out.println("Load Wikidata label mapping for the " + language.getLanguageAdjective()
					+ " Wikidata labels - " + TimeTransformer.getTime() + ".");

			Set<String> forbiddenPrefixes = this.forbiddenPrefixesPerLanguage.get(language);

			int lineNo = 0;

			LineIterator it = null;

			Database db = dbCreator.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIDATA_LABEL);

			try {
				it = FileLoader.getLineIterator(FileName.WIKIDATA_LABELS, language);
				while (it.hasNext()) {
					String line = it.nextLine();
					if (lineNo % 1000000 == 0)
						System.out.println(" Line " + lineNo + " - " + TimeTransformer.getTime() + ".");

					lineNo += 1;
					String[] parts = line.split("\t");

					String wikidataId = parts[0];
					String label = parts[1].trim();

					if (label.isEmpty())
						continue;

					int numericWikidataId = Integer.parseInt(wikidataId.substring(1));

					if (!this.validWikidataIds.contains(numericWikidataId))
						continue;

					if (!labelIsValid(label, forbiddenPrefixes)) {
						for (Database wikidataIdToWikipediaIdDB : wikidataIdToWikipediaIdDBs) {
							dbCreator.deleteEntry(wikidataIdToWikipediaIdDB, String.valueOf(numericWikidataId));
						}
					}

					// if(label.startsWith("[") && label.endsWith("]")) {
					// brackets+=1;
					// continue;
					// }

					dbCreator.createEntry(db, String.valueOf(numericWikidataId), label);

					this.usedWikidataIds.add(numericWikidataId);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it.close();
					dbCreator.closeDB(db);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println(language + ", ignoredEntities: " + ignoredEntities);
			// System.out.println(language + ", entities: " +
			// this.entitiesByWikidataNumericIds.size());

			MemoryStatsUtil.printMemoryStats();
		}

		for (Database wikidataIdToWikipediaIdDB : wikidataIdToWikipediaIdDBs)
			dbCreator.closeDB(wikidataIdToWikipediaIdDB);
		for (Database wikipediaIdToWikipediaIdDB : wikipediaIdToWikipediaIdDBs)
			dbCreator.closeDB(wikipediaIdToWikipediaIdDB);

		for (Language language : languages)
			dbCreator.closeEnvironment(language, DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_ID);

		System.out.println("ignoredEntities: " + ignoredEntities);
		// System.out.println(this.entitiesByWikidataNumericIds. + "
		// entities.");

	}

	private Set<Integer> loadAllValidWikidataEntities() {

		// valid = hasFacts and/or is not news/scientific article

		Set<String> wikipediaInternalClasses = loadWikipediaInternalClasses();

		System.out.println("loadAllWikidataEntitiesWithFacts");

		Set<Integer> wikidataIds = new HashSet<Integer>();

		BufferedReader br1 = null;

		try {

			try {
				br1 = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_FACTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br1.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t"))
					continue;

				wikidataIds.add(Integer.valueOf(parts[1].substring(1)));
				wikidataIds.add(Integer.valueOf(parts[3].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br1.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br2 = null;
		try {
			try {
				br2 = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br2.readLine()) != null) {
				String[] parts = line.split("\t");

				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br3 = null;
		try {
			try {
				br3 = FileLoader.getReader(FileName.WIKIDATA_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br3.readLine()) != null) {
				String[] parts = line.split("\t");

				try {
					wikidataIds.add(Integer.valueOf(parts[2].substring(1)));
					wikidataIds.add(Integer.valueOf(parts[0].substring(1)));
				} catch (NumberFormatException e) {
					System.out.println("Error in location file: " + line);
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br3.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br4 = null;
		try {
			try {
				br4 = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br4.readLine()) != null) {
				String[] parts = line.split("\t");

				// only add parent: We are only interested in subs if they are
				// actually the location of something. Parents are needed for
				// transitivity.
				wikidataIds.add(Integer.valueOf(parts[1].substring(1)));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br4.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br5 = null;
		try {
			try {
				br5 = FileLoader.getReader(FileName.WIKIDATA_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br5.readLine()) != null) {
				String[] parts = line.split(Config.TAB);
				wikidataIds.add(Integer.valueOf(parts[0].substring(1)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br5.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("\t" + wikidataIds.size() + " (with scientific articles)");

		// remove scientific articles
		// TODO: Do this as blacklist class

		int entitiesToIgnore = 0;

		// Ignore all entities that are in WikiNews ONLY. Some events (e.g.
		// German federal election 2017 are events AND in WikiNews.
		Set<String> entitiesWithValidClass = new HashSet<String>();
		Set<String> entitiesInWikiNews = new HashSet<String>();

		BufferedReader br6 = null;
		try {
			try {
				br6 = FileLoader.getReader(FileName.WIKIDATA_INSTANCE_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br6.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				String parentClass = parts[2];

				if (parentClass.equals(WikidataResource.WIKINEWS_ARTICLE.getId())) {
					entitiesInWikiNews.add(parts[0]);
				} else if (parentClass.equals(WikidataResource.SCIENTIFIC_ARTICLE.getId())
						|| wikipediaInternalClasses.contains(parentClass)) {
					// remove scientific article and Wikipedia internal items
					// (e.g. templates)
					wikidataIds.remove(Integer.valueOf(parts[0].substring(1)));
				} else {
					entitiesWithValidClass.add(parts[0]);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br6.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Ignore " + entitiesToIgnore
				+ " entities because they are Wikipedia internal entities or scientific article.");

		System.out.println("WikiNews entities: " + entitiesInWikiNews.size());
		entitiesInWikiNews.removeAll(entitiesWithValidClass);
		System.out.println("Ignore " + entitiesInWikiNews.size() + " entities because they are in WikiNews only.");
		entitiesWithValidClass.clear();

		for (String entity : entitiesInWikiNews) {
			wikidataIds.remove(Integer.valueOf(entity.substring(1)));
		}

		System.out.println("Valid Wikidata IDs: " + wikidataIds.size());

		return wikidataIds;
	}

	private boolean labelIsValid(String wikipediaLabel, Set<String> forbiddenPrefixes) {

		if (wikipediaLabel.contains(":")) {
			for (String forbiddenPrefix : forbiddenPrefixes) {
				if (wikipediaLabel.startsWith(forbiddenPrefix + ":"))
					return false;
			}
		}

		return true;

	}

	private Set<String> loadWikipediaInternalClasses() {

		Set<String> wikipediaInternalClasses = new HashSet<String>();

		try {
			JSONObject wikidataJSON = new JSONObject(FileLoader.readFile(FileName.WIKIDATA_WIKIPEDIA_INTERNAL_ITEMS));
			JSONArray bindings = wikidataJSON.getJSONObject("results").getJSONArray("bindings");
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject binding = bindings.getJSONObject(i);

				JSONObject itemJSON = binding.getJSONObject("item");
				String item = itemJSON.getString("value");

				item = item.substring(item.lastIndexOf("/") + 1);

				wikipediaInternalClasses.add(item);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return wikipediaInternalClasses;
	}

	public void createFileOfValidWikidataIDs() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.WIKIDATA_VALID_IDS);

			for (int wikidataId : Sets.intersection(validWikidataIds, usedWikidataIds)) {
				writer.write(wikidataId + "\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

}
