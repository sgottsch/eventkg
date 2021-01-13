package de.l3s.eventkg.integration.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.integrator.LiteralRelationsIntegrator;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.LiteralRelation;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Pipeline;

public class LiteralRelationsIntegratorTest {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public static void main(String[] args) {

		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		Pipeline.initDataSets(languages);

		try {
			createDummyDataset();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		LiteralRelationsIntegrator ri = new LiteralRelationsIntegrator(languages, null);
		ri.loadPropertyGroups();

		ri.integrateRelations();
	}

	private static void createDummyDataset() throws ParseException {

		Entity e1 = new Entity();
		e1.setId("e1");
		e1.setWikidataId("e1");
		// DataStore.getInstance().getEntities().add(e1);
		Entity e2 = new Entity();
		e2.setId("e2");
		e2.setWikidataId("e2");
		// DataStore.getInstance().getEntities().add(e2);
		Entity e3 = new Entity();
		e3.setId("e3");
		e3.setWikidataId("e3");
		// DataStore.getInstance().getEntities().add(e3);
		Entity e4 = new Entity();
		e4.setId("e4");
		e4.setWikidataId("e4");
		// DataStore.getInstance().getEntities().add(e4);

		String l1 = "l1";
		String l2 = "l2";
		String l3 = "l3";

		// e1 marriedTo e2
		LiteralRelation r1 = new LiteralRelation();
		r1.setSubject(e1);
		r1.setObject(l1);
		r1.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r1.setProperty("isMarriedTo");
		r1.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getLiteralRelations().add(r1);

		LiteralRelation r2 = new LiteralRelation();
		r2.setSubject(e1);
		r2.setObject(l1);
		r2.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-10"), DateGranularity.DAY));
		r2.setProperty("P26");
		r2.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getLiteralRelations().add(r2);

		LiteralRelation r3 = new LiteralRelation();
		r3.setSubject(e1);
		r3.setObject(l1);
		r3.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r3.setProperty("spouse");
		r3.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r3);

		LiteralRelation r3c = new LiteralRelation();
		r3c.setSubject(e1);
		r3c.setObject(l1);
		r3c.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r3c.setProperty("spouse");
		r3c.setDataSet(DataSets.getInstance().getDataSet(Language.FR, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r3c);

		LiteralRelation r3b = new LiteralRelation();
		r3b.setSubject(e1);
		r3b.setObject(l2);
		r3b.setProperty("spouse");
		r3b.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r3b);

		// single relation

		LiteralRelation r4 = new LiteralRelation();
		r4.setSubject(e1);
		r4.setObject(l3);
		r4.setProperty("isMarriedTo");
		r4.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getLiteralRelations().add(r4);

		// e2 died in e3

		LiteralRelation r5 = new LiteralRelation();
		r5.setSubject(e2);
		r5.setObject(l3);
		r5.setProperty("deathPlace");
		r5.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r5);

		LiteralRelation r6 = new LiteralRelation();
		r6.setSubject(e2);
		r6.setObject(l3);
		r6.setProperty("P20"); // place of death
		r6.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getLiteralRelations().add(r6);

		// single relation

		LiteralRelation r7 = new LiteralRelation();
		r7.setSubject(e2);
		r7.setObject(l3);
		r7.setProperty("publisher");
		r7.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r7);

		// two identical relations

		LiteralRelation r8 = new LiteralRelation();
		r8.setSubject(e2);
		r8.setObject(l3);
		r8.setProperty("abcde");
		r8.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r8);

		LiteralRelation r9 = new LiteralRelation();
		r9.setSubject(e2);
		r9.setObject(l3);
		r9.setProperty("abcde");
		r9.setDataSet(DataSets.getInstance().getDataSet(Language.FR, Source.DBPEDIA));
		DataStore.getInstance().getLiteralRelations().add(r9);

	}

}
