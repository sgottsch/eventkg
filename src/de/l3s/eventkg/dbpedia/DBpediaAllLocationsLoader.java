package de.l3s.eventkg.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaAllLocationsLoader extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public Set<Entity> locationEntities = new HashSet<Entity>();

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

	public DBpediaAllLocationsLoader(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("DBpediaAllLocationsLoader", Source.DBPEDIA, "?", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	private void extractLocations(Language language) {

		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.DBPEDIA_RELATIONS_TRANSITIVE, language);

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
					Entity entity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
							subject);
					if (entity != null) {
						this.locationEntities.add(entity);
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

	}

	private void writeResults() {

		PrintWriter writer = null;
		try {
			writer = FileLoader.getWriter(FileName.DBPEDIA_DBO_LOCATIONS);
			for (Entity entity : this.locationEntities) {
				if (entity.getWikidataId() != null)
					writer.write(entity.getWikidataId() + "\n");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

	public static Set<Entity> loadLocationEntities(WikidataIdMappings mappings) {

		Set<Entity> entities = new HashSet<Entity>();

		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.DBPEDIA_DBO_LOCATIONS);

			String line;
			while ((line = br.readLine()) != null) {
				Entity entity = mappings.getEntityByWikidataId(line);
				entity.setLocation(true);
				entities.add(entity);
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

		return entities;
	}

}
