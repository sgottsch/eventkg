package de.l3s.eventkg.integration.test;

import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.integrator.PositionsIntegrator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;

public class PositionsIntegratorTest {

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
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, Config.getValue("uri"));
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2, Config.getValue("uri"));

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

		Entity entity = getDummyEntity();

		PositionsIntegrator pi = new PositionsIntegrator(languages, null,null);
		pi.initDataSetsByTrustWorthiness();
		pi.integratePosition(entity);

		for (Position position : entity.getPositionsWithDataSets().keySet()) {
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

}
