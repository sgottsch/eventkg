package de.l3s.eventkg.wikidata.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.wikidata.processors.EventSubClassProcessor;

/**
 * Given the "subclass of" and "instance of" relations extracted from the
 * Wikidata dump, creates a file listing all event items in Wikidata.
 */
public class EventsFromFileFinder extends Extractor {

	private PrintWriter resultsWriter;

	public EventsFromFileFinder(List<Language> languages) {
		super("EventsFromFileFinder", Source.WIKIDATA,
				"Given the \"subclass of\" and \"instance of\" relations extracted from the Wikidata dump, creates a file listing all event items in Wikidata",
				languages);
	}

	public void run() {
		Set<String> eventClasses = extractSubClass();
		extractEventInstances(eventClasses);
	}

	private Set<String> extractSubClass() {

		Map<String, Set<String>> subClasses = new HashMap<String, Set<String>>();

		Map<String, String> labels = new HashMap<String, String>();

		Set<String> forbiddenClasses = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_SUBCLASS_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				String id = parts[0];

				String parentClass = parts[2];

				// ignore "Wikimedia internal stuff" children
				if (parentClass.equals("Q17442446")) {
					forbiddenClasses.add(id);
					continue;
				}

				labels.put(id, parts[1]);

				if (!subClasses.containsKey(parentClass))
					subClasses.put(parentClass, new HashSet<String>());
				subClasses.get(parentClass).add(id);
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

		// add "war -> subclass of -> armed conflict". That was missing in the
		// specific data revision and re-inserted later.
		// subClasses.get("Q350604").add("Q198");

		Set<String> targetClasses = new HashSet<String>();
		targetClasses.add("Q1190554");
		targetClasses.add("Q1656682");

		forbiddenClasses.add("Q1914636"); // activity
		forbiddenClasses.add("Q3249551"); // process
		forbiddenClasses.add("Q17442446"); // Wikimedia internal stuff
		forbiddenClasses.add("Q12139612"); // enumeration

		Set<String> allClasses = new HashSet<String>();

		boolean changed = true;
		// String indent = "";
		while (changed) {
			Set<String> newTargetClasses = new HashSet<String>();
			for (String id : targetClasses) {
				// System.out.println(indent + id + Config.TAB +
				// labels.get(id));
				if (subClasses.containsKey(id)) {
					newTargetClasses.addAll(subClasses.get(id));
					// for (String childId : subClasses.get(id))
					// System.out.println(indent + "- " + childId + Config.TAB +
					// labels.get(childId));
				}
			}
			// System.out.println(targetClasses);
			// System.out.println("\t" + newTargetClasses);
			newTargetClasses.removeAll(forbiddenClasses);
			targetClasses = newTargetClasses;

			// for (String id : newTargetClasses) {
			// System.out.println(indent + labels.get(id) + "\t" + id);
			// }

			changed = !Sets.difference(newTargetClasses, allClasses).isEmpty();

			allClasses.addAll(newTargetClasses);
			// indent += "\t";
		}

		return allClasses;
	}

	private void extractEventInstances(Set<String> eventClasses) {

		try {
			resultsWriter = FileLoader.getWriter(FileName.WIKIDATA_EVENTS);
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_INSTANCE_OF);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(EventSubClassProcessor.TAB);

				String parentClass = parts[2];

				if (eventClasses.contains(parentClass)) {

					String id = parts[0];
					String labelEn = parts[1];
					String wikiLabelEn = parts[3];

					// if (labelEn.equals("\\N"))
					// continue;

					resultsWriter.write(id + EventSubClassProcessor.TAB + labelEn + EventSubClassProcessor.TAB
							+ wikiLabelEn + "\n");
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
