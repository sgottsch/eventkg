package de.l3s.eventkg.wikidata.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

/**
 * Given the "subclass of" and "instance of" relations extracted from the
 * Wikidata dump, creates a file listing all event items in Wikidata.
 */
public class EventsFromFileFinder extends Extractor {

	private PrintWriter resultsWriter;

	public static void main(String[] args) {
		Config.init(args[0]);
		List<Language> ls = new ArrayList<Language>();
		ls.add(Language.EN);
		EventsFromFileFinder ff = new EventsFromFileFinder(ls);
		ff.run();
	}

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
		forbiddenClasses.add("Q20202269"); // music term
		forbiddenClasses.add("Q7366"); // song
		forbiddenClasses.add("Q155171"); // cover version
		forbiddenClasses.add("Q816829"); // periodization

		// Avoid cities as events: municipality ... self-governance ...
		// administration ... assembly ... meeting ... event
		forbiddenClasses.add("Q1752346"); // assembly
		forbiddenClasses.add("Q43229"); // organization
		forbiddenClasses.add("Q2495862"); // Congress

		forbiddenClasses.add("Q23893363"); // erroneous entry?, parent of
											// cultural heritage
		forbiddenClasses.add("Q17633526"); // Wikinews article
		forbiddenClasses.add("Q65943"); // theorem

		forbiddenClasses.add("Q14795564"); // determinator for date of periodic
											// occurrence

		Set<String> allClasses = new HashSet<String>();

		boolean printTree = false;

		boolean changed = true;
		String indent = "";

		while (changed) {
			Set<String> newTargetClasses = new HashSet<String>();
			for (String id : targetClasses) {

				if (printTree) {
					System.out.println(indent + id + Config.TAB + labels.get(id));
				}

				if (subClasses.containsKey(id)) {
					newTargetClasses.addAll(subClasses.get(id));

					if (printTree) {
						for (String childId : subClasses.get(id))
							System.out.println(
									indent + "- " + childId + Config.TAB + labels.get(childId) + " | parent: " + id);
					}

				}
			}
			// System.out.println(targetClasses);
			// System.out.println("\t" + newTargetClasses);
			newTargetClasses.removeAll(forbiddenClasses);
			targetClasses = newTargetClasses;

			if (printTree) {
				for (String id : newTargetClasses) {
					System.out.println(indent + labels.get(id) + "\t" + id);
				}
			}

			changed = !Sets.difference(newTargetClasses, allClasses).isEmpty();

			allClasses.addAll(newTargetClasses);

			if (printTree)
				indent += "\t";
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

				String[] parts = line.split(Config.TAB);

				String parentClass = parts[2];

				if (eventClasses.contains(parentClass)) {

					String id = parts[0];
					String labelEn = parts[1];
					String wikiLabelEn = parts[3];

					resultsWriter.write(id + Config.TAB + labelEn + Config.TAB + wikiLabelEn + Config.TAB + parentClass
							+ Config.NL);
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
