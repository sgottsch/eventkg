package de.l3s.eventkg.statistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class CountEventsWithLocations {

	public static void main(String[] args) {

		SimpleDateFormat sta = new SimpleDateFormat("yyyy-MM-dd");
		Date date;
		try {
			date = sta.parse("2017-03-11");
			SimpleDateFormat standardFormat = new SimpleDateFormat("\"yyyy-MM-dd\"'^^xsd:date'");
			System.out.println(standardFormat.format(date));

		} catch (ParseException e2) {
			e2.printStackTrace();
		}

		Config.init("config_eventkb_local.txt");

		Set<String> eventsWithLocYAGO = new HashSet<String>();
		Set<String> eventsWithLocDBPedia = new HashSet<String>();
		Set<String> eventsWithLocWikidata = new HashSet<String>();
		Set<String> eventsWithLocWikipedia = new HashSet<String>();
		Set<String> eventsWithLocWCE = new HashSet<String>();

		Set<String> eventsWithTimeYAGO = new HashSet<String>();
		Set<String> eventsWithTimeDBPedia = new HashSet<String>();
		Set<String> eventsWithTimeWikidata = new HashSet<String>();
		Set<String> eventsWithTimeWikipedia = new HashSet<String>();
		Set<String> eventsWithTimeWCE = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				if (!line.startsWith("<"))
					continue;

				String[] parts = line.split(" ");
				String event = parts[0];
				String prop = parts[1];
				String dataSet = parts[3];

				if (prop.equals("so:location")) {
					if (dataSet.contains("dbpedia_en"))
						eventsWithLocDBPedia.add(event);
					else if (dataSet.contains("yago"))
						eventsWithLocYAGO.add(event);
					else if (dataSet.contains("wikidata"))
						eventsWithLocWikidata.add(event);
					else if (dataSet.contains("wikipedia"))
						eventsWithLocWikipedia.add(event);
					else if (dataSet.contains("wce"))
						eventsWithLocWCE.add(event);
				} else {
					if (dataSet.contains("dbpedia_en"))
						eventsWithTimeDBPedia.add(event);
					else if (dataSet.contains("yago"))
						eventsWithTimeYAGO.add(event);
					else if (dataSet.contains("wikidata"))
						eventsWithTimeWikidata.add(event);
					else if (dataSet.contains("wikipedia"))
						eventsWithTimeWikipedia.add(event);
					else if (dataSet.contains("wce"))
						eventsWithTimeWCE.add(event);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Times");
		Set<String> allTime = new HashSet<String>();
		allTime.addAll(eventsWithTimeDBPedia);
		allTime.addAll(eventsWithTimeYAGO);
		allTime.addAll(eventsWithTimeWikidata);
		allTime.addAll(eventsWithTimeWCE);
		allTime.addAll(eventsWithTimeWikipedia);
		System.out.println("YAGO: " + eventsWithTimeYAGO.size());
		System.out.println("DBpedia: " + eventsWithTimeDBPedia.size());
		System.out.println("Wikidata: " + eventsWithTimeWikidata.size());
		System.out.println("Wikipedia: " + eventsWithTimeWikipedia.size());
		System.out.println("WCE: " + eventsWithTimeWCE.size());
		System.out.println("All: " + allTime.size());

		System.out.println("Locations");
		Set<String> allLoc = new HashSet<String>();
		allLoc.addAll(eventsWithLocDBPedia);
		allLoc.addAll(eventsWithLocYAGO);
		allLoc.addAll(eventsWithLocWikidata);
		allLoc.addAll(eventsWithLocWCE);
		allLoc.addAll(eventsWithLocWikipedia);
		System.out.println("YAGO: " + eventsWithLocYAGO.size());
		System.out.println("DBpedia: " + eventsWithLocDBPedia.size());
		System.out.println("Wikidata: " + eventsWithLocWikidata.size());
		System.out.println("Wikipedia: " + eventsWithLocWikipedia.size());
		System.out.println("WCE: " + eventsWithLocWCE.size());
		System.out.println("All: " + allLoc.size());

	}

}
