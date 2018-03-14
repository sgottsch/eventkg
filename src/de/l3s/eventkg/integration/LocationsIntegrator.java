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
		super("LocationsIntegrator", Source.ALL,
				"Fuses locations of events into a common graph: For each event, takes the union of locations from all sources and then limits this set of locations to the sub locations (e.g., {Paris, Francy, Lyon} becomes {Paris, Lyon}.",
				languages);
	}

	public void run() {
		// System.out.println("integrateLocationsByUnion");
		// integrateLocationsByUnion();
		System.out.println("integrateLocationsMinimum");
		integrateLocationsMinimum();
	}

	// private void integrateLocationsByUnion() {
	// // simply take the union of all locations per entity
	// for (Event event : DataStore.getInstance().getEvents()) {
	// for (Entity location : event.getLocations()) {
	// DataStore.getInstance().addLocation(new Location(event,
	// DataSets.getInstance().getDataSetWithoutLanguage(Source.INTEGRATED_LOC_2),
	// location, null));
	// }
	// }
	// }

	private void integrateLocationsMinimum() {

		int i = 0;

		// simply take the union of all locations per entity
		for (Event event : DataStore.getInstance().getEvents()) {

			i += 1;

			if (i % 10000 == 0)
				System.out.println(i + "/" + DataStore.getInstance().getEvents().size());

			Set<Entity> locations = new HashSet<Entity>();
			locations.addAll(event.getLocations());

			if (locations.isEmpty())
				continue;

			// List<String> beforeLocations = new ArrayList<String>();
			// for (Entity location : locations)
			// beforeLocations.add(location.getWikipediaLabel(Language.EN));
			// System.out.println("Before: " + StringUtils.join(beforeLocations,
			// " "));

			boolean changed = true;

			while (changed) {
				changed = false;
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

			// List<String> afterLocations = new ArrayList<String>();
			// for (Entity location : locations)
			// afterLocations.add(location.getWikipediaLabel(Language.EN));
			// System.out.println("After: " + StringUtils.join(afterLocations, "
			// "));
			// if (beforeLocations.size() != afterLocations.size())
			// System.out.println("\t => changed");

			for (Entity location : locations) {
				DataStore.getInstance().addLocation(new Location(event,
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), location, null));
			}
		}
	}

}
