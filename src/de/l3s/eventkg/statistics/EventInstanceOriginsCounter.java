package de.l3s.eventkg.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventInstanceOriginsCounter {

	public static void main(String[] args) {

		Config.init("config_eventkb_local.txt");

		Map<String, Set<String>> eventsBySource = new HashMap<String, Set<String>>();

		for (String line : FileLoader.readLines(FileName.ALL_EVENT_PAGES)) {
			String[] parts = line.split("\t");
			String sourcesLine = parts[2];
			for (String str : sourcesLine.split(" \\| ")) {
				str = str.substring(0, str.indexOf("(")).trim();
				if (!eventsBySource.containsKey(str))
					eventsBySource.put(str, new HashSet<String>());
				eventsBySource.get(str).add(parts[0]);
			}

		}

		for (String source : eventsBySource.keySet()) {
			System.out.println(source + ": " + eventsBySource.get(source).size());
		}

	}

}
