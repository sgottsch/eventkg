package de.l3s.eventkg.integration.integrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.Pipeline;

public class PositionsIntegrator extends Extractor {

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();

	public static void main(String[] args) {
		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		Pipeline.initDataSets(languages);

		createDummyDataset();

		PositionsIntegrator pi = new PositionsIntegrator(languages);
		pi.integratePositions();

		for (Entity entity : DataStore.getInstance().getEntities()) {
			for (Position position : entity.getPositions()) {
				System.out.println(
						entity.getPositionsWithDataSets().get(position).getId() + ": " + position.getLatitude());
			}
		}
	}

	private static void createDummyDataset() {

		Entity entity1 = new Entity();
		DataStore.getInstance().getEntities().add(entity1);

		Position pos1 = new Position(35.7833, 37.4972);
		Position pos2 = new Position(35.783333333333, 37.497222222222);
		Position pos3 = new Position(35.7833, 37.4972);

		entity1.addPosition(pos1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		entity1.addPosition(pos2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		entity1.addPosition(pos3, DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));

	}

	public PositionsIntegrator(List<Language> languages) {
		super("PositionsIntegrator", Source.ALL, "Fuses geo positions into a common graph.", languages);
	}

	public void run() {
		System.out.println("integratePositions");
		integratePositions();
	}

	private void integratePositions() {

		initDataSetsByTrustWorthiness();

		Set<Entity> entitiesAndEvents = new HashSet<Entity>();
		entitiesAndEvents.addAll(DataStore.getInstance().getEntities());
		entitiesAndEvents.addAll(DataStore.getInstance().getEvents());

		for (Entity entity : entitiesAndEvents) {

			Position integratedPosition = null;

			if (entity.getPositions().size() == 1) {
				for (Position position : entity.getPositions()) {
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
				entity.addPosition(new Position(integratedPosition.getLatitude(), integratedPosition.getLongitude()),
						DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG));
			}

		}

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

	private void initDataSetsByTrustWorthiness() {
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
