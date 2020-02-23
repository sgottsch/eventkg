package de.l3s.eventkg.integration.integrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;

public class PositionsIntegrator extends Extractor {

	List<DataSet> dataSetsByTrustWorthiness = new ArrayList<DataSet>();
	private AllEventPagesDataSet allEventPagesDataSet;

	public static void main(String[] args) {
		DataSets.getInstance().addDataSet(Language.DE, Source.DBPEDIA, "http://de.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.FR, Source.DBPEDIA, "http://fr.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.RU, Source.DBPEDIA, "http://ru.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.PT, Source.DBPEDIA, "http://pt.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.EN, Source.DBPEDIA, "http://dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.DE, Source.WIKIPEDIA, "https://dumps.wikimedia.org/dewiki/");
		DataSets.getInstance().addDataSet(Language.FR, Source.WIKIPEDIA, "https://dumps.wikimedia.org/frwiki/");
		DataSets.getInstance().addDataSet(Language.RU, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ruwiki/");
		DataSets.getInstance().addDataSet(Language.PT, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ptwiki/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WIKIPEDIA, "https://dumps.wikimedia.org/enwiki/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.WIKIDATA,
				"https://dumps.wikimedia.org/wikidatawiki/entities/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.YAGO,
				"https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/downloads/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WCE, "http://wikitimes.l3s.de/Resource.jsp");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, "http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2,
				"http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, "http://eventkg.l3s.uni-hannover.de/");

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

		Entity entity = getDummyEntity();

		PositionsIntegrator pi = new PositionsIntegrator(languages, null);
		pi.initDataSetsByTrustWorthiness();
		pi.integratePosition(entity);

		for (Position position : entity.getPositions()) {
			System.out.println(entity.getPositionsWithDataSets().get(position).getId() + ": " + position.getLatitude());
		}
	}

	private static Entity getDummyEntity() {

		Entity entity1 = new Entity();

		Position pos1 = new Position(35.7833, 37.4972);
		Position pos2 = new Position(35.783333333333, 37.497222222222);
		Position pos3 = new Position(35.7833, 37.4972);

		entity1.addPosition(pos1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		entity1.addPosition(pos2, DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		entity1.addPosition(pos3, DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));

		return entity1;
	}

	public PositionsIntegrator(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("PositionsIntegrator", Source.ALL, "Fuses geo positions into a common graph.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("integratePositions");
		integratePositions();
	}

	private void integratePositions() {

		initDataSetsByTrustWorthiness();

		for (Entity entity : this.allEventPagesDataSet.getWikidataIdMappings().getEntitiesByWikidataNumericIds()
				.values()) {
			integratePosition(entity);
		}

		for (int wikidataId : this.allEventPagesDataSet.getWikidataIdsOfAllEvents()) {
			Event event = this.allEventPagesDataSet.getEventByNumericWikidataId(wikidataId);
			integratePosition(event);
		}

	}

	private void integratePosition(Entity entity) {

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
