package de.l3s.eventkg.integration.db;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;

public class DatabaseCreator {

	// In case of error: Delete DB via cmd:
	// find db -name \*.lck -type f -delete
	// find db -name \*.jdb -type f -delete

	private Map<Language, Map<DatabaseName, Environment>> dbEnvironments = new HashMap<Language, Map<DatabaseName, Environment>>();
	// private Environment languageIndependentEnvironment;
	private Map<Language, Map<DatabaseName, Database>> databasesByLanguage = new HashMap<Language, Map<DatabaseName, Database>>();
	// private Map<DatabaseName, Database> databases = new HashMap<DatabaseName,
	// Database>();

	public Database getDB(Language language, DatabaseName dbName) {

		Database db = null;

		if (databasesByLanguage.containsKey(language))
			db = databasesByLanguage.get(language).get(dbName);

		if (db != null)
			return db;

		try {

			Environment environment = null;
			if (dbEnvironments.containsKey(dbName))
				environment = dbEnvironments.get(dbName).get(language);

			if (environment == null) {
				// Open the environment, creating one if it does not exist
				EnvironmentConfig envConfig = new EnvironmentConfig();

				String folderName = Config.getValue("db_folder") + "/";
				if (dbName.getSubFolder() != null)
					folderName += dbName.getSubFolder() + "/";
				folderName += language.getLanguageLowerCase();

				if ((new File(folderName)).exists()) {
					envConfig.setAllowCreate(true);
					envConfig.setTransactional(true);
					environment = new Environment(new File(folderName), envConfig);

					System.out.println("Created environment " + language + ", " + dbName + ".");

					if (!dbEnvironments.containsKey(language))
						dbEnvironments.put(language, new HashMap<DatabaseName, Environment>());

					dbEnvironments.get(language).put(dbName, environment);
				} else
					throw new DatabaseException("No folder for environment " + folderName + ".");
			}

			// Open the database, creating one if it does not exist
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setDeferredWrite(true);
			db = environment.openDatabase(null, dbName.getName(), dbConfig);

			if (!databasesByLanguage.containsKey(language))
				databasesByLanguage.put(language, new HashMap<DatabaseName, Database>());
			databasesByLanguage.get(language).put(dbName, db);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}

		return db;
	}

	public Database getDB(DatabaseName dbName) {

		return getDB(Language.ALL, dbName);

		// Database db = null;
		//
		// if (databases.containsKey(dbName))
		// db = databases.get(dbName);
		//
		// if (db != null)
		// return db;
		//
		// try {
		//
		// if (this.languageIndependentEnvironment == null) {
		// // Open the environment, creating one if it does not exist
		// EnvironmentConfig envConfig = new EnvironmentConfig();
		// envConfig.setAllowCreate(true);
		//
		// String folderName = Config.getValue("db_folder") + "/";
		// if (dbName.getSubFolder() != null)
		// folderName += dbName.getSubFolder() + "/";
		// folderName += "all";
		//
		// System.out.println(
		// "Open environment " + new File(folderName).getPath() + " / " + new
		// File(folderName).getName());
		//
		// this.languageIndependentEnvironment = new Environment(new
		// File(folderName), envConfig);
		// }
		//
		// // Open the database, creating one if it does not exist
		// DatabaseConfig dbConfig = new DatabaseConfig();
		// dbConfig.setAllowCreate(true);
		//
		// System.out.println("Open database " + dbName.getName() + " in
		// environment "
		// + languageIndependentEnvironment.getHome().getPath() + " / "
		// + languageIndependentEnvironment.getHome().getName());
		//
		// db = this.languageIndependentEnvironment.openDatabase(null,
		// dbName.getName(), dbConfig);
		//
		// databases.put(dbName, db);
		// } catch (DatabaseException e) {
		// e.printStackTrace();
		// }
		//
		// return db;
	}

	public void createEntry(Database db, String key, String data) {

		try {
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry theData = new DatabaseEntry(data.getBytes("UTF-8"));

			db.put(null, theKey, theData);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void closeDB(Database db) {

		System.out.println("Close DB " + db + ".");

		try {
			if (db != null) {
				db.sync();
				db.close();
			}

			// if (environment != null) {
			// environment.close();
			// }
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	public void closeLanguageIndependentEnvironment(DatabaseName dbName) {
		try {
			if (dbEnvironments == null || !dbEnvironments.containsKey(Language.ALL)
					|| !dbEnvironments.get(Language.ALL).containsKey(dbName))
				System.out.println("Can't close environment.");
			System.out.println("Close environment " + dbEnvironments.get(Language.ALL).get(dbName));
			dbEnvironments.get(Language.ALL).get(dbName).close();

		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	public void closeEnvironment(Language language, DatabaseName dbName) {
		try {
			System.out.println("Close environment " + language + " - " + dbEnvironments.get(language).get(dbName));

			if (dbEnvironments == null)
				System.out.println("Can't close environment (" + language + "). dbEnvironments == null");
			else if (!dbEnvironments.containsKey(language))
				System.out.println(
						"Can't close environment (" + language + "). !dbEnvironments.containsKey(" + language + ")");
			else if (!dbEnvironments.get(language).containsKey(dbName)) {
				System.out.println("Can't close environment (" + language + "). !dbEnvironments.get(" + language
						+ ").containsKey(" + dbName + ")");
			}

			dbEnvironments.get(language).get(dbName).close();

		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	public void deleteEntry(Database db, String key) {
		try {
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
			db.delete(null, theKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getEntry(Database db, String key) {

		try {
			// Create two DatabaseEntry instances:
			// theKey is used to perform the search
			// theData will hold the value associated to the key, if found
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry theData = new DatabaseEntry();

			// Call get() to query the database
			if (db.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {

				// Translate theData into a String.
				byte[] retData = theData.getData();
				String foundData = new String(retData, "UTF-8");
				return foundData;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
