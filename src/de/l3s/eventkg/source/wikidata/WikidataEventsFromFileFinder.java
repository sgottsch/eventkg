package de.l3s.eventkg.source.wikidata;

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
import de.l3s.eventkg.util.MapUtil;
import edu.stanford.nlp.util.StringUtils;

/**
 * Given the "subclass of" and "instance of" relations extracted from the
 * Wikidata dump, creates a file listing all event items in Wikidata.
 */
public class WikidataEventsFromFileFinder extends Extractor {

	private PrintWriter resultsWriter;
	private PrintWriter blacklistResultsWriter;

	private boolean printTree = false;
	private Map<String, Set<String>> allTransitiveParentClasses = new HashMap<String, Set<String>>();

	private Map<String, String> labels = new HashMap<String, String>();

	public static void main(String[] args) {
		Config.init(args[0]);
		List<Language> ls = new ArrayList<Language>();
		ls.add(Language.EN);
		WikidataEventsFromFileFinder ff = new WikidataEventsFromFileFinder(ls);
		ff.run();
	}

	public WikidataEventsFromFileFinder(List<Language> languages) {
		super("WikidataEventsFromFileFinder", Source.WIKIDATA,
				"Given the \"subclass of\" and \"instance of\" relations extracted from the Wikidata dump, creates a file listing all event items in Wikidata",
				languages);
	}

	public void run() {
		Set<String> eventClasses = extractSubClasses();
		extractEventInstances(eventClasses);
	}

	private Set<String> extractSubClasses() {

		Map<String, Set<String>> subClasses = new HashMap<String, Set<String>>();

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
				if (parentClass.equals(WikidataResource.WIKIMEDIA_INTERNAL_STUFF.getId())) {
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
		targetClasses.add(WikidataResource.OCCURRENCE.getId());
		targetClasses.add(WikidataResource.EVENT.getId());

		for (String line : FileLoader.readLines(FileName.WIKIDATA_EVENT_BLACKLIST_CLASSES)) {
			forbiddenClasses.add(line.split("\t")[0]);
		}

		Set<String> allClasses = new HashSet<String>();

		boolean changed = true;
		String indent = "";

		while (changed) {
			Set<String> newTargetClasses = new HashSet<String>();
			for (String id : targetClasses) {

				if (printTree) {
					if (!this.allTransitiveParentClasses.containsKey(id)) {
						this.allTransitiveParentClasses.put(id, new HashSet<String>());
						this.allTransitiveParentClasses.get(id).add(id);
					}
					System.out.println(indent + id + Config.TAB + labels.get(id));
				}

				if (subClasses.containsKey(id)) {
					newTargetClasses.addAll(subClasses.get(id));

					if (printTree) {
						for (String childId : subClasses.get(id)) {
							System.out.println(
									indent + "- " + childId + Config.TAB + labels.get(childId) + " | parent: " + id);

							if (!this.allTransitiveParentClasses.containsKey(childId)) {
								this.allTransitiveParentClasses.put(childId, new HashSet<String>());
								this.allTransitiveParentClasses.get(childId).add(childId);
							}
							this.allTransitiveParentClasses.get(childId).add(id);

						}
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

		if (printTree) {

			// make all parents transitive
			changed = true;
			while (changed) {
				changed = false;
				Map<String, Set<String>> newParents = new HashMap<String, Set<String>>();
				for (String childId : this.allTransitiveParentClasses.keySet()) {
					for (String id : this.allTransitiveParentClasses.get(childId)) {
						for (String parentId : this.allTransitiveParentClasses.get(id)) {
							if (!this.allTransitiveParentClasses.get(childId).contains(parentId)) {
								if (!newParents.containsKey(childId))
									newParents.put(childId, new HashSet<String>());
								newParents.get(childId).add(parentId);
							}
						}
					}
				}
				if (!newParents.isEmpty()) {
					changed = true;
					for (String childId : newParents.keySet()) {
						for (String parentId : newParents.get(childId))
							this.allTransitiveParentClasses.get(childId).add(parentId);
					}
				}
			}

		}

		return allClasses;
	}

	private void extractEventInstances(Set<String> eventClasses) {

		Set<String> blacklistClasses = new HashSet<String>();
		blacklistClasses.add(WikidataResource.DETERMINATOR_FOR_DATE_OF_PERIODIC_OCCURRENCE.getId());
		blacklistClasses.add(WikidataResource.HUMAN.getId());
		blacklistClasses.add(WikidataResource.FICTIONAL_HUMAN.getId());

		Map<String, Integer> instancesCount = new HashMap<String, Integer>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.WIKIDATA_EVENTS);
			blacklistResultsWriter = FileLoader.getWriter(FileName.WIKIDATA_NO_EVENTS);
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

				if (blacklistClasses.contains(parentClass)) {
					String id = parts[0];
					String labelEn = parts[1];
					String wikiLabelEn = parts[3];

					blacklistResultsWriter.write(id + Config.TAB + labelEn + Config.TAB + wikiLabelEn + Config.TAB
							+ parentClass + Config.NL);
				}

				if (eventClasses.contains(parentClass)) {

					if (printTree) {
						for (String classId : this.allTransitiveParentClasses.get(parentClass)) {
							if (!instancesCount.containsKey(classId))
								instancesCount.put(classId, 1);
							else
								instancesCount.put(classId, instancesCount.get(classId) + 1);
						}
					}

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
				blacklistResultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (printTree) {
			System.out.println("ALL EVENT CLASSES");
			for (String eventClass : MapUtil.sortByValueDescending(instancesCount).keySet()) {
				// collect parents map
				Map<String, Integer> parents = new HashMap<String, Integer>();
				for (String parent : this.allTransitiveParentClasses.get(eventClass)) {
					parents.put(parent, instancesCount.get(parent));
				}
				List<String> topParents = new ArrayList<String>();
				int i = 0;
				for (String parent : MapUtil.sortByValueDescending(parents).keySet()) {
					topParents.add(labels.get(parent) + ": " + parents.get(parent));
					i += 1;
					if (i >= 5)
						break;
				}
				String parentCounts = StringUtils.join(topParents, "; ");
				System.out.println(labels.get(eventClass) + " (" + instancesCount.get(eventClass) + ") - " + eventClass
						+ "\n\t" + parentCounts);
			}
		}

	}

}
