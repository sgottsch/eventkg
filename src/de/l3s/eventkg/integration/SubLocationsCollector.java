package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class SubLocationsCollector extends Extractor {

	private Set<Entity> locations = new HashSet<Entity>();
	private AllEventPagesDataSet allEventPagesDataSet;

	public SubLocationsCollector(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("SubLocationsCollector", Source.ALL, "?", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Load sub locations.");
		loadSubLocations();
	}

	private void loadSubLocations() {

		System.out.println("EntitiesByWikidataNumericIds 2: "
				+ this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataNumericIds().size());

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_SUB_LOCATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t");

				String entity1WikidataId = parts[0];
				String entity2WikidataId = parts[2];

				Entity location1 = this.allEventPagesDataSet.getWikidataIdMappings()
						.getEntityByWikidataId((entity1WikidataId));
				Entity location2 = this.allEventPagesDataSet.getWikidataIdMappings()
						.getEntityByWikidataId((entity2WikidataId));

				if (location1 == null) {
					System.out.println("Missing location 1: " + entity1WikidataId);
					continue;
				}
				if (location2 == null) {
					System.out.println("Missing location 2: " + entity2WikidataId);
					continue;
				}
			
				this.locations.add(location1);
				this.locations.add(location2);

				location1.addSubLocation(location2);
				location2.addParentLocation(location1);

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

		System.out.println("#Locations with sub locations: " + this.locations.size());
		for (Entity location : this.locations) {
			DataCollector.collectAllParents(location, location.getParentLocations());
			// no self loops!
			location.getAllParentLocations().remove(location);
		}

	}

}
