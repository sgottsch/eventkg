package de.l3s.eventkg.integration.test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sleepycat.je.Database;

import de.l3s.eventkg.integration.EventKGDBCreator;
import de.l3s.eventkg.integration.db.DatabaseCreator;
import de.l3s.eventkg.integration.db.DatabaseName;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;

public class DBTest {

	public static void main(String[] args) throws UnsupportedEncodingException {
		Config.init(args[0]);
		DatabaseCreator dbc = new DatabaseCreator();

		List<Language> languages = new ArrayList<Language>();
		// languages.add(Language.ALL);
		languages.add(Language.EN);
		languages.add(Language.DE);
		languages.add(Language.FR);
		languages.add(Language.IT);
		languages.add(Language.PT);
		languages.add(Language.RU);
		languages.add(Language.DA);
		languages.add(Language.HR);

		testWikidataMapping(dbc);
		testWikidataMappingCurrent(dbc);

		// dbc.listDatabases(languages);

		// System.out.println("\n\n\n --- " + Language.EN + "\t" +
		// DatabaseName.WIKIPEDIA_LABEL_TO_WIKIDATA_ID);
		// showDatabase(dbc.getDB(Language.EN,
		// DatabaseName.WIKIPEDIA_LABEL_TO_WIKIDATA_ID));
		// System.out.println("\n\n\n --- " + Language.DE + "\t" +
		// DatabaseName.WIKIPEDIA_LABEL_TO_WIKIDATA_ID);
		// showDatabase(dbc.getDB(Language.DE,
		// DatabaseName.WIKIPEDIA_LABEL_TO_WIKIDATA_ID));
		// System.out.println("\n\n\n --- " + Language.EN + "\t" +
		// DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_LABEL);
		// showDatabase(dbc.getDB(Language.EN,
		// DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_LABEL));
		// System.out.println("\n\n\n --- " + Language.DE + "\t" +
		// DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_LABEL);
		// showDatabase(dbc.getDB(Language.DE,
		// DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_LABEL));

		Map<Integer, String> exampleWikidataIds = new LinkedHashMap<Integer, String>();
		exampleWikidataIds.put(4582410, "1985 NCAA Women's Division I Basketball Tournament");
		exampleWikidataIds.put(151340, "Battle of France");
		exampleWikidataIds.put(176883, "FIFA World Cup");
		exampleWikidataIds.put(1078119, "2010 FIFA U-17 Women's World Cup");

		exampleWikidataIds.put(6534, "French Revolution");

		exampleWikidataIds.put(97, "Atlantic Ocean");
		exampleWikidataIds.put(29, "chess variant");
		exampleWikidataIds.put(117157, "Gulf of Cádiz");
		exampleWikidataIds.put(203950, "Cape Trafalgar");

		exampleWikidataIds.put(52670482, "Linuxwochen 2018");

		for (Language language : languages) {
			System.out.println("=== " + language);
			Database db = dbc.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIPEDIA_ID);
			Database db2 = dbc.getDB(language, DatabaseName.WIKIDATA_ID_TO_WIKIDATA_LABEL);

			for (int wikidataId : exampleWikidataIds.keySet()) {
				String wikipediaId = dbc.getEntry(db, String.valueOf(wikidataId));
				String wikidataLabel = dbc.getEntry(db2, String.valueOf(wikidataId));

				System.out.println(language + "\t" + wikidataId + " (" + exampleWikidataIds.get(wikidataId) + ")\t"
						+ wikipediaId + "\t" + wikidataLabel);

				if (wikipediaId != null) {
					Database db3 = dbc.getDB(language, DatabaseName.WIKIPEDIA_ID_TO_WIKIDATA_ID);
					System.out.println(" -> " + dbc.getEntry(db3, wikipediaId));
				}

			}
		}

	}

	private static void testWikidataMapping(DatabaseCreator dbc) {

		System.out.println("Test Wikidata mapping to old EventKG version.");

		System.out.println(
				"13291867? -> " + dbc.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID), "203950"));
		System.out.println("Last entity ID: "
				+ dbc.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID), EventKGDBCreator.LAST_ID));

		System.out.println(
				"666531? -> " + dbc.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID), "171416"));
		System.out.println("Last event ID: "
				+ dbc.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID), EventKGDBCreator.LAST_ID));

		System.out.println("event_656989? -> "
				+ dbc.getEntry(dbc.getDB(Language.EN, DatabaseName.OLD_EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID),
						"Bombards Barawa, on the coast between Kismayo and Mogadishu."));

		System.out.println("event_12011? -> " + dbc.getEntry(
				dbc.getDB(Language.FR, DatabaseName.OLD_EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID),
				"Fin de la Saison 1978-1979 de la LNH suivi des Séries éliminatoires de la Coupe Stanley 1979. les Canadiens de Montréal remportent la Coupe Stanley contre les Rangers de New York."));

	}

	public static void testWikidataMappingCurrent(DatabaseCreator dbc) {

		System.out.println("Test Wikidata mapping to current EventKG version.");

		Map<Integer, String> exampleWikidataIds = new LinkedHashMap<Integer, String>();
		exampleWikidataIds.put(4582410, "1985 NCAA Women's Division I Basketball Tournament");
		exampleWikidataIds.put(151340, "Battle of France");
		exampleWikidataIds.put(176883, "FIFA World Cup");
		exampleWikidataIds.put(1078119, "2010 FIFA U-17 Women's World Cup");

		exampleWikidataIds.put(97, "Atlantic Ocean");
		exampleWikidataIds.put(29, "chess variant");
		exampleWikidataIds.put(117157, "Gulf of Cádiz");
		exampleWikidataIds.put(203950, "Cape Trafalgar");

		exampleWikidataIds.put(52670482, "Linuxwochen 2018");

		for (int wikidataId : exampleWikidataIds.keySet()) {
			System.out.println(wikidataId + " - " + exampleWikidataIds.get(wikidataId));
			System.out.println(" Current, entity: " + dbc
					.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_ENTITY_ID), String.valueOf(wikidataId)));

			System.out.println(" Current, event: " + dbc
					.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_EVENT_ID), String.valueOf(wikidataId)));

			System.out.println(" Previous, entity: " + dbc.getEntry(
					dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID), String.valueOf(wikidataId)));

			System.out.println(" Previous, event: " + dbc
					.getEntry(dbc.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID), String.valueOf(wikidataId)));

		}

	}

	// private static void showDatabase(Database db, Integer lineLimit, String
	// subString)
	// throws UnsupportedEncodingException {
	//
	// try {
	// System.out.println("DB " + db.getDatabaseName() + ", " + lineLimit + ", "
	// + subString);
	// } catch (DatabaseException e) {
	// System.out.println("DB, " + lineLimit + ", " + subString);
	// }
	//
	// Cursor myCursor = null;
	//
	// try {
	// myCursor = db.openCursor(null, null);
	//
	// // Cursors returns records as pairs of DatabaseEntry objects
	// DatabaseEntry foundKey = new DatabaseEntry();
	// DatabaseEntry foundData = new DatabaseEntry();
	//
	// // Retrieve records with calls to getNext() until the
	// // return status is not OperationStatus.SUCCESS
	// int i = 0;
	// while (myCursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
	// OperationStatus.SUCCESS) {
	// String keyString = new String(foundKey.getData(), "UTF-8");
	// String dataString = new String(foundData.getData(), "UTF-8");
	// if (subString == null || (keyString.contains(subString) ||
	// dataString.contains(subString)))
	// System.out.println("Key| Data : " + keyString + " | " + dataString + "");
	// if (lineLimit != null && i == lineLimit)
	// break;
	// i += 1;
	// }
	// } catch (DatabaseException de) {
	// System.err.println("Error reading from database: " + de);
	// } finally {
	// try {
	// if (myCursor != null) {
	// myCursor.close();
	// }
	// } catch (DatabaseException dbe) {
	// System.err.println("Error closing cursor: " + dbe.toString());
	// }
	// }
	//
	// }

}
