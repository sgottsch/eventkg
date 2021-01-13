package de.l3s.eventkg.integration.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.integrator.RelationsIntegrator;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Pipeline;

public class RelationsIntegratorTest {

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

		RelationsIntegrator ri = new RelationsIntegrator(languages, null);
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

		// e1 marriedTo e2
		GenericRelation r1 = new GenericRelation();
		r1.setSubject(e1);
		r1.setObject(e2);
		r1.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r1.setProperty("isMarriedTo");
		r1.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getGenericRelations().add(r1);

		GenericRelation r2 = new GenericRelation();
		r2.setSubject(e1);
		r2.setObject(e2);
		r2.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-10"), DateGranularity.DAY));
		r2.setProperty("P26");
		r2.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getGenericRelations().add(r2);

		GenericRelation r3 = new GenericRelation();
		r3.setSubject(e1);
		r3.setObject(e2);
		r3.setStartTime(new DateWithGranularity(dateFormat.parse("2007-07-17"), DateGranularity.DAY));
		r3.setProperty("spouse");
		r3.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r3);

		GenericRelation r3b = new GenericRelation();
		r3b.setSubject(e1);
		r3b.setObject(e2);
		r3b.setProperty("spouse");
		r3b.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r3b);

		// single relation

		GenericRelation r4 = new GenericRelation();
		r4.setSubject(e1);
		r4.setObject(e3);
		r4.setProperty("isMarriedTo");
		r4.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO));
		DataStore.getInstance().getGenericRelations().add(r4);

		// e2 died in e3

		GenericRelation r5 = new GenericRelation();
		r5.setSubject(e2);
		r5.setObject(e3);
		r5.setProperty("deathPlace");
		r5.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r5);

		GenericRelation r6 = new GenericRelation();
		r6.setSubject(e2);
		r6.setObject(e3);
		r6.setProperty("P20"); // place of death
		r6.setDataSet(DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		DataStore.getInstance().getGenericRelations().add(r6);

		// single relation

		GenericRelation r7 = new GenericRelation();
		r7.setSubject(e2);
		r7.setObject(e3);
		r7.setProperty("publisher");
		r7.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r7);

		// two identical relations

		GenericRelation r8 = new GenericRelation();
		r8.setSubject(e2);
		r8.setObject(e3);
		r8.setProperty("abcde");
		r8.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r8);

		GenericRelation r9 = new GenericRelation();
		r9.setSubject(e2);
		r9.setObject(e3);
		r9.setProperty("abcde");
		r9.setDataSet(DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA));
		DataStore.getInstance().getGenericRelations().add(r9);

	}

}
