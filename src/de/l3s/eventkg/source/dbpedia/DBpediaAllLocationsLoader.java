package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaAllLocationsLoader extends Extractor {

	public Map<Language, Set<String>> locationEntities = new HashMap<Language, Set<String>>();
	public Map<Language, Set<String>> noLocationEntities = new HashMap<Language, Set<String>>();

	public static void main(String[] args) {
		Config.init("config_eventkb_local.txt");
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}
	}

	@Override
	public void run() {
		for (Language language : languages) {
			extractLocations(language);
		}
		writeResults();
	}

	public DBpediaAllLocationsLoader(List<Language> languages) {
		super("DBpediaAllLocationsLoader", Source.DBPEDIA,
				"Extracts all DBpedia entities that represent locations (type <http://dbpedia.org/ontology/Place>).",
				languages);
	}

	private void extractLocations(Language language) {

		Map<String, Set<String>> parentClasses = DBpediaTypesExtractor.parseOntology();

		this.locationEntities.put(language, new HashSet<String>());
		this.noLocationEntities.put(language, new HashSet<String>());
		BufferedReader br = null;

		if (FileLoader.fileExists(FileName.DBPEDIA_TYPES, language)) {

			try {
				br = FileLoader.getReader(FileName.DBPEDIA_TYPES, language);

				String line;
				while ((line = br.readLine()) != null) {
					String[] parts = line.split(" ");
					String type = parts[2];

					String subject = parts[0];
					if (!subject.contains("resource"))
						continue;

					try {
						subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					} catch (StringIndexOutOfBoundsException e) {
						continue;
					}

					if (type.equals("<http://dbpedia.org/ontology/Place>")) {
						this.locationEntities.get(language).add(subject);
					} else {
						type = type.substring(type.lastIndexOf("/") + 1, type.lastIndexOf(">"));
						if (parentClasses.containsKey(type) && parentClasses.get(type).contains("Place"))
							this.locationEntities.get(language).add(subject);
						else
							this.noLocationEntities.get(language).add(subject);
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

		// any object that can be mapped to a GeoNames entity is a location as
		// well
		// not part anymore of monthly DBpedia dumps
		// try {
		// br = FileLoader.getReader(FileName.DBPEDIA_GEONAMES_LINKS, language);
		//
		// String line;
		// while ((line = br.readLine()) != null) {
		//
		// String[] parts = line.split(" ");
		//
		// String subject = parts[0];
		// if (!subject.contains("resource"))
		// continue;
		//
		// try {
		// subject = subject.substring(subject.lastIndexOf("resource/") + 9,
		// subject.lastIndexOf(">"));
		// } catch (StringIndexOutOfBoundsException e) {
		// continue;
		// }
		//
		// this.locationEntities.get(language).add(subject);
		//
		// }
		//
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// try {
		// br.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }

		System.out.println("locationEntities " + language + " II: " + locationEntities.size());
	}

	private void writeResults() {

		for (Language language : this.languages) {
			PrintWriter writer = null;
			try {
				writer = FileLoader.getWriter(FileName.DBPEDIA_ALL_LOCATIONS, language);
				for (String entityLabel : this.locationEntities.get(language)) {
					writer.write(entityLabel + "\n");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}
		}

		for (Language language : this.languages) {
			PrintWriter writer = null;
			try {
				writer = FileLoader.getWriter(FileName.DBPEDIA_NO_LOCATIONS, language);
				for (String entityLabel : this.noLocationEntities.get(language)) {
					writer.write(entityLabel + "\n");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}
		}

	}

	public static Set<Entity> loadLocationEntities(List<Language> languages, WikidataIdMappings mappings) {

		// location type assigment in DBpedia is not perfect. Ignore assignment
		// if more languages say it is NOT a loataion.

		Set<Entity> locationEntities = new HashSet<Entity>();

		BufferedReader br = null;

		for (Language language : languages) {
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_ALL_LOCATIONS, language);

				String line;
				while ((line = br.readLine()) != null) {

					// manual correction
					if (line.equals("Eierwurf_von_Halle"))
						continue;

					Entity entity = mappings.getEntityByWikipediaLabel(language, line);

					if (entity == null)
						continue;

					entity.increaseIsLocationCount();

					locationEntities.add(entity);
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

			try {
				br = FileLoader.getReader(FileName.DBPEDIA_NO_LOCATIONS, language);

				String line;
				while ((line = br.readLine()) != null) {

					// manual correction
					if (line.equals("Eierwurf_von_Halle"))
						continue;

					Entity entity = mappings.getEntityByWikipediaLabel(language, line);

					if (entity == null)
						continue;

					entity.increaseIsNoLocationCount();
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

		System.out.println(" Number of location entities before filtering: " + locationEntities.size());

		for (Iterator<Entity> it = locationEntities.iterator(); it.hasNext();) {
			Entity entity = it.next();

			if (entity.getNoLocationCount() > entity.getLocationCount()) {
				it.remove();
				continue;
			}

			entity.setLocation(true);
		}

		System.out.println(" Number of location entities after filtering: " + locationEntities.size());

		return locationEntities;
	}

}
