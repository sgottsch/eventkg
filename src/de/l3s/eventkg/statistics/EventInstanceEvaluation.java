package de.l3s.eventkg.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventInstanceEvaluation {

	public static void main(String[] args) {

		Config.init("config_eventkb_local.txt");

		Map<String, List<String>> eventsBySource = new HashMap<String, List<String>>();

		// int ia = 0;
		for (String line : FileLoader.readLines(FileName.ALL_EVENT_PAGES)) {
			String[] parts = line.split("\t");
			String sourcesLine = parts[2];
			for (String str : sourcesLine.split(" \\| ")) {
				String oriStr = str;
				str = str.substring(0, str.indexOf("(")).trim();
				if (!eventsBySource.containsKey(str))
					eventsBySource.put(str, new ArrayList<String>());
				eventsBySource.get(str).add(parts[1] + "   " + oriStr);
			}
			// ia += 1;
			// if (ia > 100)
			// break;
		}

		for (String source : eventsBySource.keySet()) {
			System.out.println("\n\n\n");
			System.out.println(source);
			System.out.println(
					"Category\tDetails\tSingle/series\tNotes\tEnglish\tGerman\tRussian\tFrench\tPortuguese\tLink\tSource");
			int i = 0;
			Collections.shuffle(eventsBySource.get(source));

			for (String event : eventsBySource.get(source)) {
				String line = "\t\t\t\t";

				String reason = event.split("   ")[1];
				// reason = reason.substring(reason.indexOf("(") + 1,
				// reason.lastIndexOf(")"));
				// reason = reason.replace("_", " ");

				String[] reasonParts = reason.split(" ");
				String reasonString = "";
				// System.out.println(reason);
				for (int ib = 0; ib < reasonParts.length; ib += 2) {
					if (reasonParts[ib].equals(source)) {
						String oneReason = reasonParts[ib + 1].substring(1, reasonParts[ib + 1].length() - 1);
						reasonString += oneReason.replace("_", " ") + " ";
					}
				}
				reasonString = reasonString.trim();

				event = event.split("   ")[0];
				String link = null;
				boolean hasLabel = false;

				for (String label : event.split(" ")) {
					String language = label.substring(0, 2);
					label = label.substring(3);
					if (label.equals("-"))
						label = " ";
					else {
						hasLabel = true;
						if (link == null) {
							link = "https://" + language + ".wikipedia.org/wiki/" + label.replace(" ", "_");
						}
					}
					line += label.replace("_", " ") + "\t";
				}

				if (!hasLabel)
					continue;

				i += 1;

				line += link + "\t" + reasonString;
				System.out.println(line);
				if (i == 100)
					break;
			}
		}

	}

}
