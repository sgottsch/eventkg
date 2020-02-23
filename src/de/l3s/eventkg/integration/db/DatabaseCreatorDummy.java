package de.l3s.eventkg.integration.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;

public class DatabaseCreatorDummy {

	private Map<Language, Environment> dbEnvironments = new HashMap<Language, Environment>();
	private Environment languageIndependentEnvironment;
	private Map<Language, Map<DatabaseName, Database>> databasesByLanguage = new HashMap<Language, Map<DatabaseName, Database>>();
	private Map<DatabaseName, Database> databases = new HashMap<DatabaseName, Database>();

	public Database getDB(Language language, DatabaseName dbName) {
		
		System.out.println("Get DB "+language+", "+dbName.getName());

		Database db = null;

		if (databasesByLanguage.containsKey(language))
			db = databasesByLanguage.get(language).get(dbName);

		if (db != null)
			return db;

		Environment environment = dbEnvironments.get(language);

		if (environment == null) {

			String folderName = Config.getValue("db_folder") + "/";

			if (dbName.getSubFolder() != null)
				folderName += dbName.getSubFolder() + "/";
			folderName += language.getLanguageLowerCase();

			System.out.println(" -> Env.: " + folderName);

			dbEnvironments.put(language, environment);
		}

		if (!databasesByLanguage.containsKey(language))
			databasesByLanguage.put(language, new HashMap<DatabaseName, Database>());
		databasesByLanguage.get(language).put(dbName, db);

		return db;
	}

	public void listDatabases(List<Language> languages) {
		System.out.println("---");
	}

	public Database getDB(DatabaseName dbName) {

		Database db = null;

		if (databases.containsKey(dbName))
			db = databases.get(dbName);

		if (db != null)
			return db;

		if (this.languageIndependentEnvironment == null) {
			// Open the environment, creating one if it does not exist

			String folderName = Config.getValue("db_folder") + "/";
			if (dbName.getSubFolder() != null)
				folderName += dbName.getSubFolder() + "/";
			folderName += "all";

			System.out.println("New env. at: " + folderName);
		}

		// Open the database, creating one if it does not exist

		databases.put(dbName, db);

		return db;
	}

	public void createEntry(Database db, String key, String data) {

		// System.out.println("Create entry " + key + " -> " + data);

	}

	public void closeDB(Database db) {
	}

	public void closeLanguageIndependentEnvironment() {
	}

	public void closeEnvironment(Language language) {
	}

	public String getEntry(Database db, String key) {
		System.out.println("Get entry " + key);

		return null;
	}

}
