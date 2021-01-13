package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import com.sleepycat.je.Database;

import de.l3s.eventkg.integration.db.DatabaseCreator;
import de.l3s.eventkg.integration.db.DatabaseName;
import de.l3s.eventkg.integration.test.DBTest;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventKGDBCreator {

	public static final String LAST_ID = "lastId";
	private DatabaseCreator dbCreator = new DatabaseCreator();

	public boolean loadFromPreviousVersion;
	private List<Language> languages;

	private boolean readPreviousVersionFromLightOutput = false;
	private boolean readCurrentVersionFromLightOutput = true;

	// private int a = 0;
	// private int b = 0;

	private DatabaseName descriptionsDatabaseName;

	public EventKGDBCreator(List<Language> languages, boolean loadFromPreviousVersion) {
		this.languages = languages;
		this.loadFromPreviousVersion = loadFromPreviousVersion;
	}

	public void createWikidataMappingIDDB() {

		Set<Integer> eventIds = new HashSet<Integer>();

		System.out.println("Create mapping from Wikidata ID to EventKG ID.");

		// first file needs to be event!
		Map<FileName, Database> files = new LinkedHashMap<FileName, Database>();

		Integer numberOfEventFiles = 1;

		if (loadFromPreviousVersion) {
			System.out.println(" Load from previous EventKG version.");
			files.put(FileName.ALL_TTL_EVENTS_PREVIOUS_VERSION,
					dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID));
			files.put(FileName.ALL_TTL_TEXT_EVENTS_PREVIOUS_VERSION,
					dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID));
			files.put(FileName.ALL_TTL_ENTITIES_PREVIOUS_VERSION,
					dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID));
			this.descriptionsDatabaseName = DatabaseName.OLD_EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID;
			numberOfEventFiles = 2;
		} else {
			System.out.println(" Load from current EventKG version.");
			files.put(FileName.ALL_TTL_EVENTS, dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_EVENT_ID));
			files.put(FileName.ALL_TTL_ENTITIES, dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_ENTITY_ID));
			this.descriptionsDatabaseName = DatabaseName.EVENTKG_EVENT_DESCRIPTION_TO_EVENTKG_ID;
			numberOfEventFiles = 1;
		}

		// We only need to load textual events when we are in the
		// PREVIOUS_VERSION mode, because textual events are an independent step
		// at the end.

		boolean readLightOutput = readPreviousVersionFromLightOutput;
		if (!loadFromPreviousVersion)
			readLightOutput = readCurrentVersionFromLightOutput;

		boolean isEventsFile = true;
		int lastId = 0;

		try {
			for (FileName fileName : files.keySet()) {

				// a = 0;

				System.out.println(" Process file " + fileName.getFileName() + ".");

				Database db = files.get(fileName);
				int lineNumber = 0;
				LineIterator it = null;
				try {

					if (readLightOutput)
						it = FileLoader.getLineIteratorLight(fileName);
					else
						it = FileLoader.getLineIterator(fileName);

					while (it.hasNext()) {

						if (lineNumber % 500000 == 0)
							System.out.println(" Line " + lineNumber);

						String line = it.nextLine();
						lineNumber += 1;

						if (line.isEmpty() || line.startsWith("@"))
							continue;

						String[] parts = line.split(" ");

						if (isEventsFile && (parts[1].equals("dcterms:description")
								|| parts[1].equals("<http://purl.org/dc/terms/description>"))) {
							parseEventDescription(line, parts, eventIds, readLightOutput);
							continue;
						}

						if (parts.length < 2)
							System.out.println("Error in " + fileName + " with line " + lineNumber + ": " + line);

						String entityId = parts[0];
						entityId = entityId.replace(Config.getResourceURI(), "");
						entityId = entityId.substring(1, entityId.length() - 1);

						int entityNo = Integer.valueOf(entityId.substring(entityId.lastIndexOf("_") + 1));
						if (entityNo > lastId)
							lastId = entityNo;

						if (!parts[1].equals("owl:sameAs")
								&& !parts[1].equals("<http://www.w3.org/2002/07/owl#sameAs>"))
							continue;

						// Wikidata IDs only
						if (!(loadFromPreviousVersion && readPreviousVersionFromLightOutput
								|| !loadFromPreviousVersion && readCurrentVersionFromLightOutput)) {
							if (!parts[parts.length - 2].equals("eventKG-g:wikidata")
									&& !parts[parts.length - 2].equals(Config.getGraphURI("wikidata")))
								continue;
						} else {
							if (!parts[2].startsWith("<http://www.wikidata.org/entity/Q"))
								continue;
						}

						if (isEventsFile)
							eventIds.add(entityNo);

						String wikidataIdString = parts[2];
						wikidataIdString = wikidataIdString.substring(1, wikidataIdString.length() - 1);
						int wikidataId = Integer
								.parseInt(wikidataIdString.substring(wikidataIdString.indexOf("/Q") + 2));

						dbCreator.createEntry(db, String.valueOf(wikidataId), entityId);
						// if (a < 50)
						// System.out.println("A: " + String.valueOf(wikidataId)
						// + "->" + entityId);
						// a += 1;
					}

					numberOfEventFiles -= 1;

					if (numberOfEventFiles <= 0) {
						isEventsFile = false;
						System.out.println(" last ID: " + lastId);
						dbCreator.createEntry(db, LAST_ID, String.valueOf(lastId));
					}

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						it.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} finally {

			if (loadFromPreviousVersion) {
				dbCreator.closeDB(dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_EVENT_ID));
				dbCreator.closeDB(dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID));
				dbCreator.closeLanguageIndependentEnvironment(DatabaseName.WIKIDATA_ID_TO_OLD_EVENTKG_ENTITY_ID);
			} else {
				dbCreator.closeDB(dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_EVENT_ID));
				dbCreator.closeDB(dbCreator.getDB(DatabaseName.WIKIDATA_ID_TO_EVENTKG_ENTITY_ID));
				dbCreator.closeLanguageIndependentEnvironment(DatabaseName.WIKIDATA_ID_TO_EVENTKG_ENTITY_ID);
				// dbCreator.closeLanguageIndependentEnvironment(DatabaseName.WIKIDATA_ID_TO_EVENTKG_EVENT_ID);
			}

			for (Language language : languages) {
				dbCreator.closeDB(dbCreator.getDB(language, this.descriptionsDatabaseName));
				dbCreator.closeEnvironment(language, this.descriptionsDatabaseName);
			}
		}

		System.out.println(" Finished: Init entity ID mapping");

		testDB();
	}

	private void testDB() {
		System.out.println("Test database.");
		if (!loadFromPreviousVersion) {
			DatabaseCreator dbc = new DatabaseCreator();
			DBTest.testWikidataMappingCurrent(dbc);
		}
	}

	// TODO: Remove line and tc parameter
	private void parseEventDescription(String line, String[] parts, Set<Integer> eventIds,
			boolean readFromLightOutput) {

		String eventId = parts[0];
		eventId = eventId.replace(Config.getResourceURI(), "");
		eventId = eventId.substring(1, eventId.length() - 1);

		int eventNo = Integer.valueOf(eventId.substring(eventId.lastIndexOf("_") + 1));

		if (!eventIds.contains(eventNo)) {
			// only collect descriptions of events that are NOT mapped to
			// Wikidata IDs

			// only collect descriptions from WCE and Wikipedia
			if (readFromLightOutput && !parts[parts.length - 2].contains("wce")
					&& !parts[parts.length - 2].contains("wikipedia"))
				return;

			// if (b < 50) {
			// System.out.println("B: " + line);
			// }

			String languageString = parts[parts.length - 2].substring(parts[parts.length - 2].lastIndexOf("_") + 1);
			languageString = languageString.replace(">", "");

			Language language = Language.getLanguage(languageString);

			Database db = this.dbCreator.getDB(language, this.descriptionsDatabaseName);

			List<String> descriptionParts = new ArrayList<String>();
			for (int i = 2; i < parts.length - 2; i++) {
				descriptionParts.add(parts[i]);
			}
			String description = StringUtils.join(descriptionParts, " ");
			description = description.substring(1, description.lastIndexOf("\""));

			// if (b < 50) {
			// System.out.println(" " + String.valueOf(description) + "->" +
			// String.valueOf(eventNo));
			// System.out.println(" " + language + ", " + eventIds.size());
			// b += 1;
			// }

			this.dbCreator.createEntry(db, description, String.valueOf(eventNo));
		}

	}

}
