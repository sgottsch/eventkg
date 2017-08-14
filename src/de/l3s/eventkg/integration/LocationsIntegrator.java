package de.l3s.eventkg.integration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class LocationsIntegrator extends Extractor {

	public LocationsIntegrator(List<Language> languages) {
		super("LocationsIntegrator", Source.ALL, "Integrate so:location into a common graph.", languages);
	}

	public void run() {
		integrateLocations();
		integrateLocationsMinimum();
	}

	private void integrateLocations() {
		// simply take the union of all locations per entity
		for (Event event : DataStore.getInstance().getEvents()) {
			for (Entity location : event.getLocations()) {
				DataStore.getInstance().addLocation(new Location(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED), location, null));
			}
		}
	}

	private void integrateLocationsMinimum() {

		// simply take the union of all locations per entity
		for (Event event : DataStore.getInstance().getEvents()) {

			Set<Entity> locations = new HashSet<Entity>();
			locations.addAll(event.getLocations());

			boolean changed = true;

			while (changed) {
				Set<Entity> parentLocationsToRemove = new HashSet<Entity>();
				for (Entity location : locations) {
					Set<Entity> parentLocations = location.getAllParentLocations();
					parentLocationsToRemove = Sets.intersection(parentLocations, locations);
					if (!parentLocationsToRemove.isEmpty())
						break;
				}
				if (!parentLocationsToRemove.isEmpty()) {
					changed = true;
					locations.removeAll(parentLocationsToRemove);
				}
			}

			for (Entity location : locations) {
				DataStore.getInstance().addLocation(new Location(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGR_LOC_MIN), location, null));
			}
		}
	}

}
