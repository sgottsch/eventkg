package de.l3s.eventkg.integration.test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Relation;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.util.TimeTransformer;

public class TemporalRelationsCollectorTest {

	public static void main(String[] args) {
		Config.init("config_eventkb_local.txt");

		List<String> lines = new ArrayList<String>();
		lines.add("Q7747$617d5c94-4189-a564-0249-74561db922aa\tQ7747\tP1344\tQ176698");
		lines.add("\tP585\t+00000002003-06-01T00:00:00Z");

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

		WikidataIdMappings wikidataIdMappings = new WikidataIdMappings(languages);
		wikidataIdMappings.loadTemporalProperties();

		Relation previousRelation = null;

		Set<Relation> relations = new HashSet<Relation>();

		for (String line : lines) {

			String[] parts = line.split("\t");

			if (line.startsWith("\t")) {

				if (previousRelation == null)
					continue;

				String propertyWikidataId = parts[1];
				String timeString = parts[2];

				TimeSymbol type = wikidataIdMappings.getWikidataTemporalPropertyTypeById(propertyWikidataId);

				if (type == TimeSymbol.START_TIME || type == TimeSymbol.START_AND_END_TIME) {
					DateWithGranularity startTime;
					try {
						startTime = TimeTransformer.generateEarliestTimeForWikidata(timeString);
						if (startTime != null && previousRelation != null)
							previousRelation.setStartTime(startTime);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				if (type == TimeSymbol.END_TIME || type == TimeSymbol.START_AND_END_TIME) {
					DateWithGranularity endTime;
					try {
						endTime = TimeTransformer.generateLatestTimeForWikidata(timeString);
						if (endTime != null && previousRelation != null)
							previousRelation.setEndTime(endTime);
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}

			} else {

				String entity1WikidataId = parts[1];
				String entity2WikidataId = parts[3];
				String propertyWikidataId = parts[2];

				Entity entity1 = new Entity(entity1WikidataId);

				Entity entity2 = new Entity(entity2WikidataId);

				previousRelation = new Relation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA,
						Language.EN, false);
				relations.add(previousRelation);
			}
		}

		for (Relation relation : relations) {
			System.out.println(relation.getEntity1().getWikidataId());
			System.out.println(relation.getEntity2().getWikidataId());
			System.out.println(relation.getStartTime() + " - " + relation.getEndTime());
		}

	}

}
