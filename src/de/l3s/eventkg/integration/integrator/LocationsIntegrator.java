package de.l3s.eventkg.integration.integrator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;

public class LocationsIntegrator extends Extractor {

	private WikidataIdMappings wikidataIdMappings;
	private TriplesWriter dataStoreWriter;

	public LocationsIntegrator(List<Language> languages, TriplesWriter dataStoreWriter,
			WikidataIdMappings wikidataIdMappings) {
		super("LocationsIntegrator", Source.ALL,
				"Fuses locations of events into a common graph: For each event, takes the union of locations from all sources and then limits this set of locations to the sub locations (e.g., {Paris, Francy, Lyon} becomes {Paris, Lyon}.",
				languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.dataStoreWriter = dataStoreWriter;
	}

	public void run() {
		// System.out.println("integrateLocationsByUnion");
		// integrateLocationsByUnion();
		System.out.println("integrateLocations");
		integrateLocations();
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

	private void integrateLocations() {

		int i = 0;

		for (Entity entity : this.wikidataIdMappings.getEntities()) {
			if (entity.isEvent()) {
				integrateLocationsOfEvent((Event) entity, i);
				i += 1;
			}
		}
	}

	private void integrateLocationsOfEvent(Event event, int i) {

		Set<Entity> locations = new HashSet<Entity>();

		for (Entity location : event.getLocationsWithDataSets().keySet()) {
			locations.add(location);
			for (DataSet ds : event.getLocationsWithDataSets().get(location)) {
				dataStoreWriter.writeEventLocation(event, location, ds, false);
			}
		}

		if (i % 10000 == 0)
			System.out.println(i + "\t" + event.getWikidataId());

		if (locations.isEmpty())
			return;

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

		if (i % 10000 == 0)
			System.out.println(" -> " + locations.size());

		for (Entity location : locations) {
			dataStoreWriter.startInstance();
			dataStoreWriter.writeEventLocation(event, location,
					DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);
			dataStoreWriter.endInstance();
		}
	}

}
