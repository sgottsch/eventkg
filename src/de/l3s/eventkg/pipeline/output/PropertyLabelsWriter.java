package de.l3s.eventkg.pipeline.output;

import java.util.List;

import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.relation.PropertyLabel;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class PropertyLabelsWriter extends Extractor {

	private TriplesWriter triplesWriter;

	public PropertyLabelsWriter(List<Language> languages, TriplesWriter triplesWriter) {
		super("PropertyLabelsWriter", Source.ALL, "Writers labels of used properties.", languages);
		this.triplesWriter = triplesWriter;
	}

	@Override
	public void run() {

		for (PropertyLabel propertyLabel : DataStore.getInstance().getPropertyLabels()) {
			triplesWriter.startInstance();
			triplesWriter.writePropertyLabel(propertyLabel);
			triplesWriter.endInstance();
		}

	}

}
