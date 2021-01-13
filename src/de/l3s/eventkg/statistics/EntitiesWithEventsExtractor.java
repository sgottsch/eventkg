package de.l3s.eventkg.statistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EntitiesWithEventsExtractor {

	public static void main(String[] args) {
		Config.init(args[0]);

		Map<String, String> idToLabelEvents = new HashMap<String, String>();
		Map<String, String> idToLabelEntities = new HashMap<String, String>();
		Map<String, Set<String>> idToRelation = new HashMap<String, Set<String>>();
		Map<String, Set<String>> relationToEvents = new HashMap<String, Set<String>>();
		Map<String, String> idToStartTime = new HashMap<String, String>();

		System.out.println("ALL_TTL_EVENTS_WITH_TEXTS");
		List<String> lines1 = FileLoader.readLines(FileName.ALL_TTL_EVENTS);
		int i1 = 0;
		for (String line : lines1) {
			if (i1 % 250000 == 0)
				System.out.println("\t" + i1 + "/" + lines1.size() + " = " + ((double) (i1 / (double) lines1.size())));
			i1 += 1;
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
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.ALL_TTL_ENTITIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			int i2 = 0;
			while ((line = br.readLine()) != null) {
				if (i2 % 250000 == 0)
					System.out.println("\t" + i2);
				i2 += 1;
				if (line.isEmpty() || line.startsWith("@"))
					continue;
				String[] parts = line.split(" ");
				String id = parts[0];
				if (parts[1].equals("owl:sameAs") && parts[3].equals("eventKG-g:dbpedia_en")) {
					String label = parts[2];
					label = label.substring(1, label.length() - 1);
					label = label.substring(label.lastIndexOf("/") + 1);
					if (id.equals("<entity_12393086>"))
						System.out.println(id + " -> " + label);
					idToLabelEntities.put(id, label);
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

		System.out.println("ALL_TTL_EVENTS_BASE_RELATIONS");
		for (String line : FileLoader.readLines(FileName.ALL_TTL_EVENTS_BASE_RELATIONS)) {
			if (line.isEmpty() || line.startsWith("@"))
				continue;
			String[] parts = line.split(" ");
			String id = parts[0];
			if (parts[1].equals("sem:hasBeginTimeStamp"))
				idToStartTime.put(id, parts[2]);
		}

		System.out.println("ALL_TTL_EVENTS_LINK_RELATIONS");
		BufferedReader br3 = null;
		try {
			try {
				br3 = FileLoader.getReader(FileName.ALL_TTL_EVENTS_LINK_RELATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			int i3 = 0;
			while ((line = br3.readLine()) != null) {
				if (i3 % 250000 == 0)
					System.out.println("\t" + i3);
				i3 += 1;
				if (line.isEmpty() || line.startsWith("@"))
					continue;
				String[] parts = line.split(" ");
				if (!parts[1].equals("rdf:subject") && !parts[1].equals("rdf:object"))
					continue;
				String relationId = parts[0];
				String id = parts[2];

				if (!idToRelation.containsKey(id))
					idToRelation.put(id, new HashSet<String>());
				idToRelation.get(id).add(relationId);

				if (id.startsWith("<event")) {
					if (!relationToEvents.containsKey(relationId))
						relationToEvents.put(relationId, new HashSet<String>());
					relationToEvents.get(relationId).add(id);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br3.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter("entities_with_events.tsv");
			for (String id : idToRelation.keySet()) {

				// the id must be connected to at least one relation that is
				// connected to an event which is not the ID
				boolean isConnected = false;
				for (String relationId : idToRelation.get(id)) {
					Set<String> relationEvents = new HashSet<String>();
					relationEvents.addAll(relationToEvents.get(relationId));
					relationEvents.remove(id);
					relationEvents.retainAll(idToStartTime.keySet());

					if (!relationEvents.isEmpty()) {
						if (id.equals("<entity_12393086>"))
							System.out.println(id + ", " + relationId + " -> " + relationEvents);

						isConnected = true;
						break;
					}
				}

				if (!isConnected)
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
