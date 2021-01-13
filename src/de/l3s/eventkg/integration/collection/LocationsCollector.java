package de.l3s.eventkg.integration.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.integrator.LocationsIntegrator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.pipeline.output.RDFWriterName;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LocationsCollector extends Extractor {

	private List<Language> languages;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter dataStoreWriter;

	public LocationsCollector(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("LocationsCollector", Source.ALL, "Collects and fuses event locations into a common graph.", languages);
		this.languages = languages;
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	public void run() {
		loadLocations();
		integrateLocations();
		clear();
	}

	private void clear() {
		this.dataStoreWriter.resetNumberOfInstances(RDFWriterName.EVENT_BASE_RELATIONS);
		for (Entity entity : this.wikidataIdMappings.getEntitiesByWikidataNumericIds().values()) {
			entity.clearLocations();
			if (entity.isEvent()) {
				((Event) entity).clearLocations();
			}
		}
	}

	private void loadLocations() {
		loadEventLocations();

		SubLocationsCollector subLocationsCollector = new SubLocationsCollector(languages, dataStoreWriter,
				wikidataIdMappings);
		subLocationsCollector.run();
	}

	private void integrateLocations() {
		LocationsIntegrator locationsIntegrator = new LocationsIntegrator(languages, dataStoreWriter,
				wikidataIdMappings);
		locationsIntegrator.run();
	}

	private void loadEventLocations() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_LOCATIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String wikidataId = parts[0];

				Event event = this.wikidataIdMappings.getEventByWikidataId(wikidataId);
				if (event == null)
					continue;

				String wikidataIdLocation = parts[2];
				Entity location = wikidataIdMappings.getEntityByWikidataId(wikidataIdLocation);

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				new Location(event, dataSet, location, null); // adds location
																// to event
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
