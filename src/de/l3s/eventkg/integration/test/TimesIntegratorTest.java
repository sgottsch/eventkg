package de.l3s.eventkg.integration.test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.integrator.TimesIntegrator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;

public class TimesIntegratorTest {

	public static void main(String[] args) throws ParseException {

		DataSets.getInstance().addDataSet(Language.DE, Source.DBPEDIA, "http://de.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.FR, Source.DBPEDIA, "http://fr.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.RU, Source.DBPEDIA, "http://ru.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.PT, Source.DBPEDIA, "http://pt.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.IT, Source.DBPEDIA, "http://it.dbpedia.org/");
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
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, Config.getURL());
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2, Config.getURL());

		// ~~~

//		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);
		languages.add(Language.FR);

		TimesIntegrator timesIntegrator = new TimesIntegrator(languages, null, null);
		timesIntegrator.init();

		Entity entity1 = new Entity("Q4118977");
		entity1.addWikipediaLabel(Language.EN, "Withdrawal_of_U.S._troops_from_Iraq");

		// ~~~

//		StartTime time1 = new StartTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
//				new DateWithGranularity(dateFormat.parse("1896-12-17"), DateGranularity.DAY));
//		StartTime time2 = new StartTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
//				new DateWithGranularity(dateFormat.parse("2009-06-30"), DateGranularity.DAY));
//		StartTime time3 = new StartTime(entity1, DataSets.getInstance().getDataSet(Language.DE, Source.DBPEDIA),
//				new DateWithGranularity(dateFormat.parse("2011-12-18"), DateGranularity.DAY));
//		StartTime time4 = new StartTime(entity1, DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA),
//				new DateWithGranularity(dateFormat.parse("1895-12-17"), DateGranularity.DAY));

//		DataStore.getInstance().addStartTime(time1);
//		DataStore.getInstance().addStartTime(time2);
//		DataStore.getInstance().addStartTime(time3);
//		DataStore.getInstance().addStartTime(time4);

//		EndTime time1E = new EndTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
//				new DateWithGranularity(dateFormat.parse("1896-12-17"), DateGranularity.DAY));
//		EndTime time2E = new EndTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
//				new DateWithGranularity(dateFormat.parse("2009-06-30"), DateGranularity.DAY));
//		EndTime time3E = new EndTime(entity1, DataSets.getInstance().getDataSet(Language.DE, Source.DBPEDIA),
//				new DateWithGranularity(dateFormat.parse("2011-12-18"), DateGranularity.DAY));
//		EndTime time4E = new EndTime(entity1, DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA),
//				new DateWithGranularity(dateFormat.parse("1890-12-17"), DateGranularity.DAY));

//		DataStore.getInstance().addEndTime(time1E);
//		DataStore.getInstance().addEndTime(time2E);
//		DataStore.getInstance().addEndTime(time3E);
//		DataStore.getInstance().addEndTime(time4E);

		timesIntegrator.integrateTimes(entity1, true, 0);

		System.out.println("");

//		for (StartTime st : DataStore.getInstance().getStartTimes()) {
//			System.out.println(
//					"Start: " + st.getDataSet().getSource() + "\t" + dateFormat.format(st.getStartTime().getDate()));
//		}
//
//		for (EndTime st : DataStore.getInstance().getEndTimes()) {
//			System.out.println(
//					"End: " + st.getDataSet().getSource() + "\t" + dateFormat.format(st.getEndTime().getDate()));
//		}

	}

}
