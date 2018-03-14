package de.l3s.eventkg.statistics;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EntitiesWithStartTimeExtractor {

	public static void main(String[] args) {
		Config.init(args[0]);

		Map<String, String> idToLabelEvents = new HashMap<String, String>();
		Map<String, String> idToLabelEntities = new HashMap<String, String>();
		Map<String, String> idToStartTime = new HashMap<String, String>();
		Map<String, String> idToEndTime = new HashMap<String, String>();

		System.out.println("ALL_TTL_EVENTS_WITH_TEXTS");
		for (String line : FileLoader.readLines(FileName.ALL_TTL_EVENTS_WITH_TEXTS)) {
			if (line.isEmpty() || line.startsWith("@"))
				continue;
			String[] parts = line.split(" ");
			String id = parts[0];
			if (parts[1].equals("owl:sameAs") && parts[3].equals("eventKG-g:dbpedia_en")) {
				String label = parts[2];
				label = label.substring(1, label.length() - 1);
				label = label.substring(label.lastIndexOf("/") + 1);
				idToLabelEvents.put(id, label);
			}
		}

		System.out.println("ALL_TTL_ENTITIES_WITH_TEXTS");
		for (String line : FileLoader.readLines(FileName.ALL_TTL_ENTITIES_WITH_TEXTS)) {
			if (line.isEmpty() || line.startsWith("@"))
				continue;
			String[] parts = line.split(" ");
			String id = parts[0];
			if (parts[1].equals("owl:sameAs") && parts[3].equals("eventKG-g:dbpedia_en")) {
				String label = parts[2];
				label = label.substring(1, label.length() - 1);
				label = label.substring(label.lastIndexOf("/") + 1);
				idToLabelEntities.put(id, label);
			}
		}

		System.out.println("ALL_TTL_EVENTS_BASE_RELATIONS");
		for (String line : FileLoader.readLines(FileName.ALL_TTL_EVENTS_BASE_RELATIONS)) {
			if (line.isEmpty() || line.startsWith("@"))
				continue;
			String[] parts = line.split(" ");
			String id = parts[0];
			if (parts[1].equals("sem:hasBeginTimeStamp"))
				idToStartTime.put(id, parts[2]);
			if (parts[1].equals("sem:hasEndTimeStamp"))
				idToEndTime.put(id, parts[2]);
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter("entities_with_start_times.tsv");
			for (String id : idToStartTime.keySet()) {
				if (idToEndTime.containsKey(id) && idToEndTime.get(id).equals(idToStartTime.get(id)))
					continue;
				if (idToLabelEntities.containsKey(id)) {
					writer.write(idToLabelEntities.get(id).replaceAll("_", " ") + "\n");
				} else if (idToLabelEvents.containsKey(id)) {
					writer.write(idToLabelEvents.get(id).replaceAll("_", " ") + "\n");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}

	}

}
