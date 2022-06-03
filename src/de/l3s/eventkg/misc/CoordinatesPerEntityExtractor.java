package de.l3s.eventkg.misc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.MapUtil;

public class CoordinatesPerEntityExtractor {

	private static final String BASE_PROPERTY = "base";
	private static final String PLACE_PROPERTY = "place";
	private Set<String> places = new HashSet<String>();
	private Map<String, Coordinate> entitiesWithCoordinates = new HashMap<String, Coordinate>();
	private Set<String> propertiesBlacklist = new HashSet<String>();
	private Set<String> propertiesPrio = new HashSet<String>();

	private Set<String> entitiesThatAreWrittenToAFile = new HashSet<String>();

	private Map<String, Map<String, Set<String>>> allEntitiesWithCoordinates = new HashMap<String, Map<String, Set<String>>>();

	private EntityIdGeneratorFromFiles idGenerator;

	public static void main(String[] args) {

		Config.init(args[0]);

		// 1. if an entity has "so:latitude" and "so:longitude", just take it
		// -> create file "entity (evektg)---entity (wikidata)---coordinate"
		// 2. if the entity has "sem:hasPlace" take the location of that one,
		// -> create file "entity (eventkg)---entity (wikidata)---related
		// (eventkg)---related (wikidata)---coordinate"
		// 3. for all remaining entities: check if related entity via base
		// relation has coordinate
		// -> create file "entity---relation---related (eventkg)---related
		// (wikidata)---coordinate"
		// 4. for all remaining entities: check if related entity via non-base
		// relation has coordinates
		// -> create file "entity---relation---related (eventkg)---related
		// (wikidata)---coordinate"

		// Preprocessing: Run coordinates.sh

		String inputFileCoordinates = "base_coordinates.csv";
		String inputFilePlaces = "event_places.csv";
		String inputSameAs = "same_as_wikidata.tsv";
		String inputPlaces = "places.csv";

		String writerBase = "coordinates_base.csv";
		String writerPlace = "coordinates_via_place.csv";
		String writerBaseRelation = "coordinates_via_base_relation.csv";
		String writerOtherRelation = "coordinates_via_other_relation.csv";
		String writerAllCoordinates = "coordinates_all.csv";

		CoordinatesPerEntityExtractor cpee = new CoordinatesPerEntityExtractor();
		cpee.init(inputSameAs, inputPlaces);

		PrintWriter statsWriter = null;
		try {
			statsWriter = new PrintWriter("stats.txt");
			System.out.println("writeBaseCoordinates");
			cpee.writeBaseCoordinates(inputFileCoordinates, writerBase);
			System.out.println("writePlaceCoordinates");
			cpee.writePlaceCoordinates(inputFilePlaces, writerPlace);
			System.out.println("writeBaseRelationsCoordinates");
			cpee.writeBaseRelationsCoordinates(writerBaseRelation, statsWriter);
			System.out.println("writeOtherRelationsCoordinates");
			cpee.writeOtherRelationsCoordinates(writerOtherRelation, statsWriter);
			System.out.println("writeAllCoordnates");
			cpee.writeAllCoordinates(writerAllCoordinates, statsWriter);
			System.out.println("done");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			statsWriter.close();
		}

	}

	private void writeAllCoordinates(String allCoordinatesFileName, PrintWriter statsWriter) {

		Map<Integer, Integer> numberOfCoordinatesCount = new HashMap<Integer, Integer>();

		PrintWriter writerAllCoordinates = null;

		try {
			writerAllCoordinates = new PrintWriter(allCoordinatesFileName);
			for (String entity : this.allEntitiesWithCoordinates.keySet()) {
				List<String> parts = new ArrayList<String>();
				String wikidataId = this.idGenerator.getWikidataIdByEventKGId(entity);
				parts.add(wikidataId);
				parts.add(entity);

				int numberOfCoordinates = this.allEntitiesWithCoordinates.get(entity).size();

				if (numberOfCoordinates > 10) {
					System.out.println("Too many coordinates (prev.) for: " + wikidataId);
				}

				List<String> coordinates = new ArrayList<String>();
				for (String coordinate : this.allEntitiesWithCoordinates.get(entity).keySet()) {
					if (containsAny(this.propertiesPrio, this.allEntitiesWithCoordinates.get(entity).get(coordinate)))
						coordinates.add(coordinate);
				}
				if (coordinates.isEmpty()) {
					for (String coordinate : this.allEntitiesWithCoordinates.get(entity).keySet()) {
						coordinates.add(coordinate);
					}
				}

				numberOfCoordinates = coordinates.size();

				if (!numberOfCoordinatesCount.containsKey(numberOfCoordinates))
					numberOfCoordinatesCount.put(numberOfCoordinates, 1);
				else
					numberOfCoordinatesCount.put(numberOfCoordinates,
							numberOfCoordinatesCount.get(numberOfCoordinates) + 1);

				if (numberOfCoordinates > 10) {
					System.out.println("Too many coordinates (after) for: " + wikidataId);
					continue;
				}

				parts.add(StringUtils.join(coordinates, ";"));

				writerAllCoordinates.println(StringUtils.join(parts, " "));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writerAllCoordinates.close();
		}

		for (int numberOfCoordinates : MapUtil.sortByValueDescending(numberOfCoordinatesCount).keySet()) {
			statsWriter.println(numberOfCoordinates + " " + numberOfCoordinatesCount.get(numberOfCoordinates));
		}

	}

	private boolean containsAny(Set<String> set1, Set<String> set2) {
		return !Sets.intersection(set1, set2).isEmpty();
	}

	private void init(String inputSameAs, String inputPlaces) {
		this.idGenerator = new EntityIdGeneratorFromFiles(inputSameAs);

		loadPlaces(inputPlaces);

		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P54>");
		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P6379>");
		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P6153>");
		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P108>");
		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P166>");
		this.propertiesBlacklist.add("<http://www.wikidata.org/prop/direct/P1343>");

		this.propertiesPrio.add(BASE_PROPERTY);
		this.propertiesPrio.add(PLACE_PROPERTY);

		this.propertiesPrio.add("<http://dbpedia.org/ontology/birthPlace>");
		this.propertiesPrio.add("<http://yago-knowledge.org/resource/isCitizenOf>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P19>");
		this.propertiesPrio.add("<http://yago-knowledge.org/resource/wasBornIn>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P27>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/country>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P195>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/nationality>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P20>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/deathPlace>");

		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P131>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/locatedInArea>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/city>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/region>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/administrativeDistrict>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P119>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/headquarter>");
		this.propertiesPrio.add("<http://www.wikidata.org/prop/direct/P159>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/district>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/hometown>");
		this.propertiesPrio.add("<http://dbpedia.org/ontology/residence>");

	}

	private void loadPlaces(String inputPlaces) {
		System.out.println("Load places");
		LineIterator it = null;
		try {

			it = FileUtils.lineIterator(new File(inputPlaces), "UTF-8");

			while (it.hasNext()) {
				String line = it.nextLine();
				this.places.add(line);
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

	public void writeBaseCoordinates(String inputFileCoordinates, String outputFileName) {

		Set<String> writtenEntities = new HashSet<String>();

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFileName);

			LineIterator it = null;
			try {

				it = FileUtils.lineIterator(new File(inputFileCoordinates), "UTF-8");

				String currentEntity = null;
				Double latitude = null;
				Double longitude = null;

				while (it.hasNext()) {
					String line = it.nextLine();
					String[] parts = line.split(" ");

					String entity = parts[0];

					if (currentEntity != null && !entity.equals(currentEntity)) {
						if (longitude != null && latitude != null) {
							Coordinate coordinate = new Coordinate(latitude, longitude);
							this.entitiesWithCoordinates.put(currentEntity, coordinate);
							writeLineWithOneProperty(writer, currentEntity, coordinate, null, BASE_PROPERTY);
							writtenEntities.add(currentEntity);
						}
						longitude = null;
						latitude = null;
					}

					String type = parts[1];
					if (type.equals("longitude"))
						longitude = Double.valueOf(parts[2]);
					else if (type.equals("latitude"))
						latitude = Double.valueOf(parts[2]);

					currentEntity = entity;

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

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			writer.close();
		}

		entitiesThatAreWrittenToAFile.addAll(writtenEntities);
	}

	public void writePlaceCoordinates(String inputFilePlaces, String outputFileName) {
		Set<String> writtenEntities = new HashSet<String>();
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFileName);

			LineIterator it = null;
			try {

				it = FileUtils.lineIterator(new File(inputFilePlaces), "UTF-8");

				while (it.hasNext()) {
					String line = it.nextLine();
					String[] parts = line.split(" ");

					String entity = parts[0];
					String place = parts[1];

					Coordinate coordinate = this.entitiesWithCoordinates.get(place);

					if (coordinate != null && !this.entitiesWithCoordinates.containsKey(entity)) {
						this.entitiesWithCoordinates.put(entity, coordinate);
						writeLineWithOneProperty(writer, entity, coordinate, place, PLACE_PROPERTY);
						writtenEntities.add(entity);
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

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			writer.close();
		}

		entitiesThatAreWrittenToAFile.addAll(writtenEntities);
	}

	public void writeBaseRelationsCoordinates(String outputFileName, PrintWriter statsWriter) {
		Set<String> writtenEntities = new HashSet<String>();

		List<FileName> fileNames = new ArrayList<FileName>();
		fileNames.add(FileName.ALL_TTL_ENTITY_BASE_RELATIONS);
		fileNames.add(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);

		Map<String, Integer> propertyCounts = new HashMap<String, Integer>();

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFileName);

			for (FileName fileName : fileNames) {

				LineIterator it = null;
				try {
					it = FileLoader.getLineIteratorLight(fileName);
					while (it.hasNext()) {
						String line = it.nextLine();

						String[] parts = line.split(" ");

						String object = parts[2];
						if (!object.startsWith("<http://eventKG.l3s.uni-hannover.de/resource/"))
							continue;
						object = createId(object);

						Coordinate coordinate = this.entitiesWithCoordinates.get(object);
						if (coordinate == null)
							continue;

						String subject = createId(parts[0]);
						String property = parts[1];

						if (property.equals("<http://dbpedia.org/ontology/previousEvent>")
								|| property.equals("<http://dbpedia.org/ontology/nextEvent>"))
							continue;

						if (!this.entitiesWithCoordinates.containsKey(subject)) {
							writeLineWithOneProperty(writer, subject, coordinate, object, property);
							writtenEntities.add(subject);
							if (!propertyCounts.containsKey(property))
								propertyCounts.put(property, 1);
							else
								propertyCounts.put(property, propertyCounts.get(property) + 1);
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
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			writer.close();
		}

		statsWriter.println("=== writeBaseRelationsCoordinates ===");
		for (String property : MapUtil.sortByValueDescending(propertyCounts).keySet()) {
			statsWriter.println(property + " " + propertyCounts.get(property));
		}

		entitiesThatAreWrittenToAFile.addAll(writtenEntities);
	}

	public void writeOtherRelationsCoordinates(String outputFileName, PrintWriter statsWriter) {
		Set<String> writtenEntities = new HashSet<String>();

		List<FileName> fileNames = new ArrayList<FileName>();
		fileNames.add(FileName.ALL_TTL_ENTITIES_OTHER_RELATIONS);
		fileNames.add(FileName.ALL_TTL_ENTITIES_TEMPORAL_RELATIONS);
		fileNames.add(FileName.ALL_TTL_EVENTS_OTHER_RELATIONS);
		Map<String, Integer> propertyCounts = new HashMap<String, Integer>();

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFileName);

			for (FileName fileName : fileNames) {

				Relation currentRelation = null;

				LineIterator it = null;
				try {
					it = FileLoader.getLineIteratorLight(fileName);
					while (it.hasNext()) {
						String line = it.nextLine();

						String[] parts = line.split(" ");
						String relationId = createId(parts[0]);

						if (currentRelation == null) {
							currentRelation = new Relation(relationId);
						} else if (currentRelation != null && !relationId.equals(currentRelation.getUri())) {

							if (this.places.contains(currentRelation.getObject())) {

								// at least one of the properties is not
								// blacklisted
								boolean propertiesValid = false;
								for (String property : currentRelation.getProperties()) {
									if (!this.propertiesBlacklist.contains(property)) {
										propertiesValid = true;
										break;
									}
								}

								if (propertiesValid) {
									writtenEntities.add(currentRelation.getSubject());
									processRelation(writer, currentRelation);

									for (String property : currentRelation.getProperties()) {
										if (!propertyCounts.containsKey(property))
											propertyCounts.put(property, 1);
										else
											propertyCounts.put(property, propertyCounts.get(property) + 1);
									}
								}
							}

							currentRelation = new Relation(relationId);
						}

						String property = parts[1];
						if (property.equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#subject>"))
							currentRelation.setSubject(createId(parts[2]));
						else if (property.equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#object>"))
							currentRelation.setObject(createId(parts[2]));
						else if (property.equals("<http://semanticweb.cs.vu.nl/2009/11/sem/roleType>"))
							currentRelation.addProperty(parts[2]);

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
		} catch (

		FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			writer.close();
		}

		statsWriter.println("=== writeOtherRelationsCoordinates ===");
		for (String property : MapUtil.sortByValueDescending(propertyCounts).keySet()) {
			statsWriter.println(property + " " + propertyCounts.get(property));
		}

		// entitiesThatAreWrittenToAFile.addAll(writtenEntities);
	}

	private String createId(String id) {
		id = id.replace("<http://eventKG.l3s.uni-hannover.de/resource/", "");
		id = id.replaceAll(">$", "");
		return id;
	}

	private void processRelation(PrintWriter writer, Relation relation) {
		Coordinate coordinate = this.entitiesWithCoordinates.get(relation.getObject());

		if (coordinate != null && !this.entitiesWithCoordinates.containsKey(relation.getSubject())) {
			writeLine(writer, relation.getSubject(), coordinate, relation.getObject(), relation.getProperties());
		}
	}

	private void writeLineWithOneProperty(PrintWriter writer, String entity, Coordinate coordinate,
			String relatedEntity, String property) {
		List<String> properties = new ArrayList<String>();
		properties.add(property);
		writeLine(writer, entity, coordinate, relatedEntity, properties);
	}

	private void writeLine(PrintWriter writer, String entity, Coordinate coordinate, String relatedEntity,
			List<String> properties) {

		if (this.entitiesThatAreWrittenToAFile.contains(entity))
			return;

		List<String> parts = new ArrayList<String>();

		parts.add(entity);

		// Wikidata ID
		String wikidataId = this.idGenerator.getWikidataIdByEventKGId(entity);
		if (wikidataId == null) {
			System.out.println("Missing Wikidata ID: " + entity);
			return;
		}
		parts.add(wikidataId);

		// Coordinate
		if (coordinate == null)
			return;
		parts.add(String.valueOf(coordinate.getLatitude()));
		parts.add(String.valueOf(coordinate.getLongitude()));

		// related entity
		if (relatedEntity != null) {
			parts.add(relatedEntity);
			// Wikidata ID
			String relatedWikidataId = this.idGenerator.getWikidataIdByEventKGId(relatedEntity);
			if (relatedWikidataId == null) {
				System.out.println("Missing Wikidata ID: " + entity);
				return;
			}
			parts.add(relatedWikidataId);
		}

		String coordinateString = coordinate.getLatitude() + "," + coordinate.getLongitude();

		if (!this.allEntitiesWithCoordinates.containsKey(entity)) {
			this.allEntitiesWithCoordinates.put(entity, new HashMap<String, Set<String>>());
			this.allEntitiesWithCoordinates.get(entity).put(coordinateString, new HashSet<String>());
			this.allEntitiesWithCoordinates.get(entity).get(coordinateString).addAll(properties);
		} else {
			if (!this.allEntitiesWithCoordinates.get(entity).containsKey(coordinateString)) {
				this.allEntitiesWithCoordinates.get(entity).put(coordinateString, new HashSet<String>());
			}
			this.allEntitiesWithCoordinates.get(entity).get(coordinateString).addAll(properties);
		}

		if (properties != null) {
			parts.add(StringUtils.join(properties, ","));
		}

		String line = StringUtils.join(parts, " ");

		writer.println(line);
	}

	private class Relation {

		private String uri;
		private String subject;
		private String object;
		private List<String> properties = new ArrayList<String>();

		public Relation(String uri) {
			super();
			this.uri = uri;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public void setObject(String object) {
			this.object = object;
		}

		public String getUri() {
			return uri;
		}

		public void addProperty(String property) {
			this.properties.add(property);
		}

		public String getSubject() {
			return subject;
		}

		public String getObject() {
			return object;
		}

		public List<String> getProperties() {
			return properties;
		}

	}

	private class Coordinate {
		private double latitude;
		private double longitude;

		public Coordinate(double latitude, double longitude) {
			super();
			this.latitude = latitude;
			this.longitude = longitude;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}

	}

}
