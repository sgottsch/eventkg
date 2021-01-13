package de.l3s.eventkg.integration.integrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;

public class PositionsIntegrator extends Extractor {

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();

	private WikidataIdMappings wikidataIdMappings;
	private TriplesWriter dataStoreWriter;

	public PositionsIntegrator(List<Language> languages, TriplesWriter dataStoreWriter,
			WikidataIdMappings wikidataIdMappings) {
		super("PositionsIntegrator", Source.ALL, "Fuses geo positions into a common graph.", languages);
		this.wikidataIdMappings = wikidataIdMappings;
		this.dataStoreWriter = dataStoreWriter;
	}

	public void run() {
		System.out.println("integratePositions");
		integratePositions();
	}

	private void integratePositions() {

		initDataSetsByTrustWorthiness();

		for (Entity entity : this.wikidataIdMappings.getEntities()) {
			integratePosition(entity);
		}

	}

	public void integratePosition(Entity entity) {
		dataStoreWriter.startInstance();

		for (Position position : entity.getPositionsWithDataSets().keySet()) {
			dataStoreWriter.writePosition(entity, position, entity.getPositionsWithDataSets().get(position), false);
		}

		Position integratedPosition = null;

		if (entity.getPositionsWithDataSets().keySet().size() == 1) {
			for (Position position : entity.getPositionsWithDataSets().keySet()) {
				integratedPosition = position;
			}

		} else {
			// only integrate by source trustWorthiness
			for (DataSet dataSet : dataSetsByTrustWorthiness) {
				Set<Position> positions = entity.getPositionsOfDataSet(dataSet);
				if (positions != null && !positions.isEmpty()) {
					// one source can have multiple positions (especially
					// Wikidata)
					if (positions.size() == 1) {
						for (Position position : positions)
							integratedPosition = position;
					} else
						integratedPosition = takeMostPrecisePosition(positions);
				}
				if (integratedPosition != null)
					break;
			}
		}

		if (integratedPosition != null) {
			dataStoreWriter.writePosition(entity, integratedPosition,
					DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG), true);
		}

		dataStoreWriter.endInstance();
	}

	private Position takeMostPrecisePosition(Set<Position> positions) {

		// return the precision with the longest longitude and latitude (as
		// strings)

		Integer maxLength = null;
		Position mostPrecisePosition = null;

		for (Position position : positions) {
			int length = String.valueOf(position.getLatitude()).length()
					+ String.valueOf(position.getLongitude()).length();
			if (maxLength == null || length > maxLength) {
				maxLength = length;
				mostPrecisePosition = position;
			}
		}

		return mostPrecisePosition;
	}

	public void initDataSetsByTrustWorthiness() {
		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));

		// English language most trustworthy
		if (this.languages.contains(Language.EN))
			dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		for (Language language : this.languages) {
			if (language != Language.EN)
				dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSet(language, Source.DBPEDIA));
		}

		dataSetsByTrustWorthiness.add(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
	}

}
