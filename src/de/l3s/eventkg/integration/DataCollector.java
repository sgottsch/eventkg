package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.dbpedia.DBpediaAllLocationsLoader;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DataCollector extends Extractor {

	private Set<Event> uniqueEvents = new HashSet<Event>();

	/**
	 * Set of entities which may never be interpreted as events.
	 */
	private Set<Entity> blacklistEvents = new HashSet<Entity>();

	private WikidataIdMappings wikidataIdMappings;

	private Set<Entity> locations;
	private Set<Entity> entitiesWithPositions;

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);

		Config.init("config_eventkb_local.txt");

		DataCollector dc = new DataCollector(languages);
		dc.init();
		dc.collectEvents();
	}

	public DataCollector(List<Language> languages) {
		super("DataCollector", de.l3s.eventkg.meta.Source.ALL,
				"Integrates information collected so far into common output files. For example, a list of event entities is finally created.",
				languages);
	}

	private void init() {
		this.wikidataIdMappings = new WikidataIdMappings(this.languages);
		wikidataIdMappings.load();
		wikidataIdMappings.loadTemporalProperties();
	}

	public void run() {
		System.out.println("Load Wikidata ID mapping.");
		init();
		System.out.println("Collect entity sub/parent locations.");
		collectSubLocations();
		System.out.println("Collect event pages.");
		collectEvents();
		System.out.println("Collect \"part of\" relations.");
		collectPartOfs();
		System.out.println("Collect \"follows\" relations.");
		collectPreviousEvents();
		System.out.println("Collect \"followed by\" relations.");
		collectNextEvents();
		System.out.println("Collect event locations.");
		collectEventLocations();
		System.out.println("Collect positions.");
		collectPositions();
	}

	private void collectPositions() {
		this.entitiesWithPositions = new HashSet<Entity>();
		collectPositionsYAGO();
		collectPositionsDBpedia();
		collectPositionsWikidata();
		writePositionsToFile();
	}

	private void collectPositionsYAGO() {
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.YAGO_POSITIONS);

			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split("\t");

				YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
				yagoLabelExtractor1.extractLabel();
				if (!yagoLabelExtractor1.isValid())
					continue;

				Entity entity = getEntity(yagoLabelExtractor1.getLanguage(), yagoLabelExtractor1.getWikipediaLabel());

				if (entity != null) {

					// YAGO always only has one position, but we have to loop
					// and take that one
					Set<Position> positions = entity
							.getPositionsOfDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));

					Position position = null;
					if (positions != null)
						for (Position positionTmp : positions)
							position = positionTmp;

					if (position == null) {
						position = new Position();
						entity.addPosition(position, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
					}

					String valueString = parts[2];
					valueString = valueString.substring(0, valueString.indexOf("^"));
					double value = Double.valueOf(StringUtils.strip(valueString, "\""));

					this.entitiesWithPositions.add(entity);

					if (parts[1].equals("<hasLatitude>"))
						position.setLatitude(value);

					if (parts[1].equals("<hasLongitude>"))
						position.setLongitude(value);
				}

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

	private void collectPositionsDBpedia() {

		for (Language language : this.languages) {
			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.DBPEDIA_POSITIONS, language);

				while (it.hasNext()) {
					String line = it.nextLine();

					String[] parts = line.split("\t");

					Entity entity = getEntity(language, parts[0]);

					if (entity != null) {
						this.entitiesWithPositions.add(entity);
						Position position = new Position(Double.valueOf(parts[1]), Double.valueOf(parts[2]));
						entity.addPosition(position, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
					}

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
	}

	private void collectPositionsWikidata() {

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.WIKIDATA_POSITIONS);

			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split("\t");

				Entity entity = getEntityFromWikidataId(parts[0]);

				if (entity != null) {
					this.entitiesWithPositions.add(entity);
					Position position = new Position(Double.valueOf(parts[2]), Double.valueOf(parts[3]));
					entity.addPosition(position, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				}

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

	private void collectEventLocations() {
		collectLocationsYAGO();
		collectLocationsDBpedia();
		collectLocationsWikidata();
		writeLocationsToFile();
	}

	private void collectSubLocations() {
		collectSubLocationsWikidata();
		minimizeSubLocations();
		writeSubLocationsToFile();
	}

	private void writeSubLocationsToFile() {

		System.out.println("writeSubLocationsToFile");

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_SUB_LOCATIONS);
			for (Entity subLocation : this.locations) {
				for (Entity location : subLocation.getParentLocations()) {
					writer.write(location.getWikidataId());
					writer.write(Config.TAB);
					writer.write(location.getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(subLocation.getWikidataId());
					writer.write(Config.TAB);
					writer.write(subLocation.getWikipediaLabelsString(this.languages));
					writer.write(Config.NL);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writePositionsToFile() {

		System.out.println("writePositionsToFile");
		System.out.println(" #Entities with positions: " + entitiesWithPositions.size());

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_POSITIONS);
			for (Entity entity : entitiesWithPositions) {
				for (Position position : entity.getPositions()) {
					if (position.getLatitude() != null && position.getLongitude() != null) {
						writer.write(entity.getWikidataId());
						writer.write(Config.TAB);
						writer.write(entity.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(String.valueOf(position.getLatitude()));
						writer.write(Config.TAB);
						writer.write(String.valueOf(position.getLongitude()));
						writer.write(Config.TAB);
						writer.write(entity.getPositionsWithDataSets().get(position).getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void writeLocationsToFile() {

		System.out.println("writeLocationsToFile");

		System.out.println("uniqueEvents: " + uniqueEvents.size());

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_LOCATIONS);
			for (Event event : uniqueEvents) {
				for (Entity location : event.getLocationsWithDataSets().keySet()) {
					for (DataSet dataSet : event.getLocationsWithDataSets().get(location)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(location.getWikidataId());
						writer.write(Config.TAB);
						writer.write(location.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private void collectLocationsWikidata() {

		System.out.println("collectLocationsWikidata");

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.WIKIDATA_LOCATIONS);

			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split("\t");

				if (parts[0].length() <= 1 || parts[2].length() <= 1) {
					System.out.println("Error in location line: " + line);
					continue;
				}

				String entity2WikidataId = parts[2];

				Event event1 = findEventFromWikidataId(parts[0]);

				if (event1 != null) {
					Entity entityObject = getEntityFromWikidataId(entity2WikidataId);
					if (entityObject != null)
						event1.addLocation(entityObject,
								DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				}

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

	private void collectLocationsDBpedia() {

		for (Language language : this.languages) {

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.DBPEDIA_EVENT_LOCATIONS, language);

				while (it.hasNext()) {
					String line = it.nextLine();

					String[] parts = line.split("\t");

					String wikipedia1Label = parts[0];
					String entity2Label = parts[2];

					Event event1 = findEvent(language, wikipedia1Label);
					if (event1 != null) {
						Entity entityObject = getEntity(language, entity2Label);
						if (entityObject != null)
							event1.addLocation(entityObject,
									DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
					}

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
	}

	private void collectLocationsYAGO() {
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.YAGO_LOCATIONS);

			while (it.hasNext()) {
				String line = it.nextLine();

				String[] parts = line.split("\t");

				YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
				yagoLabelExtractor1.extractLabel();
				if (!yagoLabelExtractor1.isValid())
					continue;

				YAGOLabelExtractor yagoLabelExtractor2 = new YAGOLabelExtractor(parts[2], this.languages);
				yagoLabelExtractor2.extractLabel();
				if (!yagoLabelExtractor2.isValid())
					continue;

				Event event1 = findEvent(yagoLabelExtractor1.getLanguage(), yagoLabelExtractor1.getWikipediaLabel());

				if (event1 != null) {
					Entity entityObject = getEntity(yagoLabelExtractor2.getLanguage(),
							yagoLabelExtractor2.getWikipediaLabel());
					if (entityObject != null)
						event1.addLocation(entityObject, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
				}

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

	// private void test(String wikipediaLabel) {
	// wikipediaLabel = wikipediaLabel.replaceAll(" ", "_");
	// Event event = events.get(wikipediaLabel);
	//
	// System.out.println(event.getOneLineRepresentation());
	// for (Entity location : event.getLocations())
	// System.out.println("\tLocation: " + location.getWikipediaLabel());
	//
	// if (!event.getChildren().isEmpty()) {
	// System.out.println("Children:");
	// printChildren(event, "\t");
	// }
	//
	// if (!event.getParents().isEmpty()) {
	// System.out.println("Parents:");
	// printParents(event, "\t");
	// }
	//
	// }
	//
	// private void printParents(Event event, String indent) {
	// for (Event parent : event.getParents()) {
	// if (parent.getStartTime() != null || parent.getEndTime() != null)
	// System.out.println(indent + parent.getOneLineRepresentation());
	// printParents(parent, indent + "\t");
	// }
	// }
	//
	// private void printChildren(Event event, String indent) {
	// for (Event child : event.getChildren()) {
	// if (child.getStartTime() != null || child.getEndTime() != null)
	// System.out.println(indent + child.getOneLineRepresentation());
	// printChildren(child, indent + "\t");
	// }
	// }

	private void collectEvents() {

		// Load event blacklists first, so they can be ignored later
		System.out.println("loadDBpediaBlacklistEvents.");
		loadDBpediaBlacklistEvents();
		System.out.println("loadLocationsBlacklistEvents.");
		loadLocationsBlacklistEvents();
		System.out.println("loadWikidataBlacklistEvents.");
		loadWikidataBlacklistEvents();

		System.out.println("loadWikidataEvents.");
		loadWikidataEvents();
		System.out.println("loadDBpediaEvents.");
		loadDBpediaEvents();
		System.out.println("loadWikipediaEvents.");
		loadWikipediaEvents();

		// TODO: Reintegrate WCE events? Has errors like "Spain" and "London".
		// loadWCEEvents();

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_EVENT_PAGES);
			for (Event event : uniqueEvents) {

				if (event.getWikidataId() == null)
					continue;

				writer.write(event.getWikidataId());
				writer.write(Config.TAB);
				writer.write(wikidataIdMappings.getLabelsString(event.getNumericWikidataId()));
				writer.write(Config.TAB);
				writer.write(StringUtils.join(event.getEventInstanceComments(), " "));
				writer.write(Config.TAB);

				writer.write(Config.NL);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void collectPartOfs() {
		collectPartOfsWikidata();
		collectPartOfsDBpedia();
		writePartOfsToFile();
	}

	private void collectPreviousEvents() {
		collectPreviousEventsWikidata();
		collectPreviousEventsDBpedia();
		writePreviousEventsToFile();
	}

	private void writePreviousEventsToFile() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_PREVIOUS_EVENTS);
			for (Event event : uniqueEvents) {
				for (Event previousEvent : event.getPreviousEvents()) {
					for (DataSet dataSet : event.getPreviousEventsWithDataSets().get(previousEvent)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(previousEvent.getWikidataId());
						writer.write(Config.TAB);
						writer.write(previousEvent.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void writeNextEventsToFile() {
		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_NEXT_EVENTS);
			for (Event event : uniqueEvents) {
				for (Event nextEvent : event.getNextEvents()) {
					for (DataSet dataSet : event.getNextEventsWithDataSets().get(nextEvent)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(event.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(nextEvent.getWikidataId());
						writer.write(Config.TAB);
						writer.write(nextEvent.getWikipediaLabelsString(this.languages));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

	private void collectNextEvents() {
		collectNextEventsWikidata();
		collectNextEventsDBpedia();
		writeNextEventsToFile();
	}

	private void writePartOfsToFile() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.ALL_PART_OF_RELATIONS);
			for (Event event : uniqueEvents) {
				for (Event child : event.getChildrenWithDataSets().keySet()) {
					for (DataSet dataSet : event.getChildrenWithDataSets().get(child)) {
						writer.write(event.getWikidataId());
						writer.write(Config.TAB);
						writer.write(wikidataIdMappings.getLabelsString(event.getNumericWikidataId()));
						writer.write(Config.TAB);
						writer.write(child.getWikidataId());
						writer.write(Config.TAB);
						writer.write(wikidataIdMappings.getLabelsString(child.getNumericWikidataId()));
						writer.write(Config.TAB);
						writer.write(dataSet.getId());
						writer.write(Config.NL);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	private Event findEventFromWikidataId(String wikidataId) {

		Entity entity = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		if (entity.isEvent())
			return (Event) entity;

		return null;
	}

	private void collectPartOfsDBpedia() {

		for (Language language : this.languages) {

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.DBPEDIA_DBO_EVENT_PARTS, language);

				while (it.hasNext()) {
					String line = it.nextLine();

					String[] parts = line.split("\t");

					String wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
							.replaceAll(" ", "_");
					String wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
							.replaceAll(" ", "_");

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addParent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addChild(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
					}

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
	}

	private void collectPreviousEventsDBpedia() {

		for (Language language : this.languages) {

			System.out.println("collectPreviousEventsDBpedia " + language);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_PREVIOUS_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = null;
					String wikipedia2Label = null;

					try {
						wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
								.replaceAll(" ", "_");
						wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
								.replaceAll(" ", "_");
					} catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
						System.out.println("Error, previous event: " + line);
						continue;
					}

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addPreviousEvent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addNextEvent(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private void collectNextEventsDBpedia() {

		for (Language language : this.languages) {

			System.out.println("collectNextEventsDBpedia " + language);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_NEXT_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					String wikipedia1Label = null;
					String wikipedia2Label = null;

					try {
						wikipedia1Label = parts[0].substring(parts[0].lastIndexOf("/") + 1, parts[0].length() - 1)
								.replaceAll(" ", "_");
						wikipedia2Label = parts[2].substring(parts[2].lastIndexOf("/") + 1, parts[2].length() - 1)
								.replaceAll(" ", "_");
					} catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
						System.out.println("Error, next event: " + line);
						continue;
					}

					Event event1 = findEvent(language, wikipedia1Label);
					Event event2 = findEvent(language, wikipedia2Label);

					if (event1 != null && event2 != null) {
						event1.addNextEvent(event2, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
						event2.addPreviousEvent(event1, DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private void collectPartOfsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_PART_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addParent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addChild(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader br2 = null;
		try {
			try {
				br2 = FileLoader.getReader(FileName.WIKIDATA_PART_OF_SERIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br2.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addParent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addChild(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

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

	}

	private void collectNextEventsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_FOLLOWED_BY);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addNextEvent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addPreviousEvent(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void collectPreviousEventsWikidata() {

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_FOLLOWS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId1 = parts[0];
				String wikidataId2 = parts[2];

				Event event1 = findEventFromWikidataId(wikidataId1);
				Event event2 = findEventFromWikidataId(wikidataId2);

				if (event1 == null || event2 == null)
					continue;

				event1.addPreviousEvent(event2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
				event2.addNextEvent(event1, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void loadDBpediaEvents() {

		for (Language language : this.languages) {

			int numberOfDBpediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_EVENTS_FILE_NAME, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					// Fix some errors in the data where the entity label is
					// e.g.
					// "Netherlands_at_the_1924_Summer_Olympics__June_9,_1924__1",
					// which should just be
					// "Netherlands_at_the_1924_Summer_Olympics"
					if (wikiLabel.contains("__"))
						continue;
					// wikiLabel = wikiLabel.substring(0,
					// wikiLabel.indexOf("__"));

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Event event = createEvent(language, wikiLabel,
							DataSets.getInstance().getDataSet(language, Source.DBPEDIA), line.split(Config.TAB)[2]);
					if (event != null)
						numberOfDBpediaEvents += 1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Number of events extracted from DBpedia (" + language.getLanguageLowerCase() + "): "
					+ numberOfDBpediaEvents);
		}

	}

	private void loadDBpediaBlacklistEvents() {
		for (Language language : this.languages) {

			int numberOfDBpediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_DBO_NO_EVENTS_FILE_NAME, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					// Fix some errors in the data where the entity label is
					// e.g.
					// "Netherlands_at_the_1924_Summer_Olympics__June_9,_1924__1",
					// which should just be
					// "Netherlands_at_the_1924_Summer_Olympics"
					if (wikiLabel.contains("__"))
						continue;
					// wikiLabel = wikiLabel.substring(0,
					// wikiLabel.indexOf("__"));

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Entity noEvent = createBlacklistEvent(language, wikiLabel, "loadDBpediaBlacklistEvents");

					if (noEvent != null) {
						numberOfDBpediaEvents += 1;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Number of blacklist events extracted from DBpedia (" + language.getLanguageLowerCase()
					+ "): " + numberOfDBpediaEvents);
		}
	}

	private void loadLocationsBlacklistEvents() {

		// Locations may never be events.

		for (Entity entity : DBpediaAllLocationsLoader.loadLocationEntities(this.languages, wikidataIdMappings)) {
			createBlacklistEventByWikidataID(entity.getWikidataId(), "loadDBpediaLocationsBlacklistEvents");
		}

	}

	private void loadWikidataBlacklistEvents() {

		// Locations may never be events.

		Set<String> wikidataBlackListEvents = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_NO_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				createBlacklistEventByWikidataID(parts[0], "loadWikidataBlacklistEvents");
				wikidataBlackListEvents.add(parts[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Number of blacklist events extracted from Wikidata : " + wikidataBlackListEvents.size());
	}

	private Event createEvent(Language language, String wikipediaLabel, DataSet dataSet, String eventInstance) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		String comment = dataSet.getId() + " (" + eventInstance + ")";

		// ignore if it's a blacklist event
		if (blacklistEvents.contains(entity))
			return null;

		if (entity.isEvent()) {
			Event event = (Event) entity;
			if (comment != null)
				event.addEventInstanceComment(comment);
			event.addDataSetAndEventInstance(dataSet, eventInstance);
			return event;
		}

		Event newEvent = new Event(entity, this.wikidataIdMappings);

		if (comment != null)
			newEvent.addEventInstanceComment(comment);
		newEvent.addDataSetAndEventInstance(dataSet, eventInstance);

		uniqueEvents.add(newEvent);

		return newEvent;
	}

	private Entity createBlacklistEventByWikidataID(String wikidataId, String comment) {

		Entity entity = getEntityFromWikidataId(wikidataId);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		blacklistEvents.add(entity);

		return entity;
	}

	private Entity createBlacklistEvent(Language language, String wikipediaLabel, String comment) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		blacklistEvents.add(entity);

		return entity;
	}

	private Event findEvent(Language language, String wikipediaLabel) {

		Entity entity = getEntity(language, wikipediaLabel);

		if (entity == null) {
			// System.out.println("Missing entity for Wikipedia label: " +
			// language + " - " + wikipediaLabel);
			return null;
		}

		if (entity.isEvent())
			return (Event) entity;

		return null;
	}

	private Event createEventByWikidataId(String wikidataId, DataSet dataSet, String eventInstance) {

		Entity entity = getEntityFromWikidataId(wikidataId);
		String comment = dataSet.getId() + " (" + eventInstance + ")";

		if (blacklistEvents.contains(entity))
			return null;

		if (entity == null)
			return null;

		if (entity.isEvent()) {
			Event event = (Event) entity;
			if (comment != null)
				event.addEventInstanceComment(comment);
			return event;
		}

		Event newEvent = new Event(entity, this.wikidataIdMappings);

		if (comment != null)
			newEvent.addEventInstanceComment(comment);

		uniqueEvents.add(newEvent);

		return newEvent;
	}

	private Entity getEntity(Language language, String wikipediaLabel) {
		Entity ent = this.wikidataIdMappings.getEntityByWikipediaLabel(language, wikipediaLabel);

		return ent;
	}

	private Entity getEntityFromWikidataId(String wikidataId) {
		Entity ent = this.wikidataIdMappings.getEntityByWikidataId(wikidataId);

		return ent;
	}

	@SuppressWarnings("unused")
	private void loadWCEEvents() {
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WCE_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String wikiLabel = line;

				wikiLabel = wikiLabel.replaceAll(" ", "_");

				createEvent(Language.EN, wikiLabel, DataSets.getInstance().getDataSet(Language.EN, Source.WCE),
						"Event");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void loadWikipediaEvents() {

		for (Language language : this.languages) {

			int numberOfWikipediaEvents = 0;

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.WIKIPEDIA_EVENTS, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String wikiLabel = line.split(Config.TAB)[0].replaceAll(" ", "_");

					if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
						continue;

					Event event = createEvent(language, wikiLabel,
							DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA), line.split(Config.TAB)[1]);
					if (event != null)
						numberOfWikipediaEvents += 1;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			System.out.println("Number of events extracted from Wikipedia (" + language.getLanguageLowerCase() + "): "
					+ numberOfWikipediaEvents);
		}

	}

	private void loadWikidataEvents() {

		int numberOfWikidataEvents = 0;

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_EVENTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);
				String wikiLabel = parts[2].replaceAll(" ", "_");

				// TODO: Is this correct?
				wikiLabel = wikiLabel.replaceAll(" ", "_");
				if (wikiLabel.startsWith("List_of_") || wikiLabel.startsWith("Lists_of_"))
					continue;

				// manual correction
				if (parts[0].equals("Q3136955"))
					continue;

				Event event = createEventByWikidataId(parts[0],
						DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA), line.split(Config.TAB)[3]);

				if (event != null)
					numberOfWikidataEvents += 1;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Number of events extracted from Wikidata: " + numberOfWikidataEvents);
	}

	private void collectSubLocationsWikidata() {

		System.out.println("collectSubLocationsWikidata");

		this.locations = DBpediaAllLocationsLoader.loadLocationEntities(this.languages, this.wikidataIdMappings);

		int found = 0;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				String type = parts[0];
				String entity1WikidataId = parts[1];
				String entity2WikidataId = parts[3];

				Entity location1 = getEntityFromWikidataId(entity1WikidataId);

				if (location1 == null) {
					// entity not found (no name in any of the given langauges)
					continue;
				}

				Entity location2 = getEntityFromWikidataId(entity2WikidataId);
				if (location2 == null) {
					// entity not found (no name in any of the given langauges)
					continue;
				}

				if (location1 == location2)
					continue;

				if (location1.isLocation() && location2.isLocation()) {
					found += 1;
					if (type.equals(Config.SUB_LOCATION_SYMBOL)) {
						location1.addParentLocation(location2);
						location2.addSubLocation(location1);
					} else if (type.equals(Config.PARENT_LOCATION_SYMBOL)) {
						location2.addParentLocation(location1);
						location1.addSubLocation(location2);
					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("SubLocs: " + found);
	}

	private void minimizeSubLocations() {

		System.out.println("minimizeSubLocations");

		// transitive parents
		for (Entity location : this.locations) {
			collectAllParents(location, location.getParentLocations());
			// no self loops!
			location.getAllParentLocations().remove(location);
		}

		// if we have
		// Brandenburger Tor, parentLocation: Berlin
		// and
		// Brandenburger Tor, parentLocation: Germany
		// and
		// Berlin, parenLocation: Germany
		// only keep row 1 & 3

		for (Entity startLocation : this.locations) {
			// union of all the parent locations of each location
			Set<Entity> allParentLocations = new HashSet<Entity>();
			for (Entity parentLocation : startLocation.getParentLocations())
				allParentLocations.addAll(parentLocation.getAllParentLocations());

			// only keep those locations which are not in that union
			startLocation.getParentLocations().removeAll(allParentLocations);
		}

		// // use symmetry for sub locations
		// for (Entity startLocation : this.locations) {
		// for (Entity parent : startLocation.getParentLocations()) {
		// parent.addSubLocation(startLocation);
		// }
		// }
	}

	public static void collectAllParents(Entity location, Set<Entity> parents) {

		for (Entity parent : parents) {
			if (!location.getAllParentLocations().contains(parent)) {
				location.addAllParentLocation(parent);
				collectAllParents(location, parent.getParentLocations());
			}
		}

	}

}
