package de.l3s.eventkg.integration.integrator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.Location;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class LocationsIntegrator extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public LocationsIntegrator(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("LocationsIntegrator", Source.ALL,
				"Fuses locations of events into a common graph: For each event, takes the union of locations from all sources and then limits this set of locations to the sub locations (e.g., {Paris, Francy, Lyon} becomes {Paris, Lyon}.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
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
			integrateLocationsOfEvent(event, i);
			i += 1;
		}

		for (int wikidataId : this.allEventPagesDataSet.getWikidataIdsOfAllEvents()) {
			Event event = this.allEventPagesDataSet.getEventByNumericWikidataId(wikidataId);
			integrateLocationsOfEvent(event, i);
			i += 1;
		}
	}

	private void integrateLocationsOfEvent(Event event, int i) {

		boolean tc = false;
		if (event.getNumericWikidataId() != null && event.getNumericWikidataId() == 171416) {
			System.out.println("TEST CASE: " + event.getWikidataId());
			tc = true;
		}

		Set<Entity> locations = new HashSet<Entity>();
		locations.addAll(event.getLocations());

		if (tc) {
			for (Entity loc : locations) {
				System.out.println(" Loc: " + loc.getWikidataId());
			}
		}

		if (i % 10000 == 0)
			System.out.println(i + "\t" + event.getWikidataId() + "\t" + event.getLocations().size() + " / "
					+ DataStore.getInstance().getLocations().size());

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
			DataStore.getInstance().addLocation(new Location(event,
					DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), location, null));
		}
	}

}
