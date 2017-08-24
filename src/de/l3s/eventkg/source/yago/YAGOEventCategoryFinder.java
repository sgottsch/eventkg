package de.l3s.eventkg.source.yago;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.source.yago.model.Category;
import de.l3s.eventkg.source.yago.model.WikipediaCategory;
import de.l3s.eventkg.source.yago.model.YAGOCategory;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class YAGOEventCategoryFinder {

	private Map<String, Map<String, Category>> categoriesByTypeAndTitle = new HashMap<String, Map<String, Category>>();

	private Set<String> subNotEventCategoryNames;

	public static void main(String[] args) {
		YAGOEventCategoryFinder eventFinder = new YAGOEventCategoryFinder();
		eventFinder.findEventCategories();
		eventFinder.findTopCategories();
	}

	private void findTopCategories() {
		loadOntologyFromFile();
		Category topCategory = categoriesByTypeAndTitle.get("owl").get("owl:Thing");
		System.out.println(topCategory.getCompleteTitle());

		for (Category child1 : topCategory.getChildren()) {
			if (child1.getType().equals("wordnet")) {
				System.out.println(child1.getCompleteTitle());
				for (Category child2 : child1.getChildren()) {
					if (child2.getType().equals("wordnet")) {
						System.out.println("\t" + child2.getCompleteTitle());
						for (Category child3 : child2.getChildren()) {
							if (child3.getType().equals("wordnet")) {
								System.out.println("\t\t" + child3.getCompleteTitle());
							}
						}
					}
				}
			}
		}

	}

	public Set<String> getNoEventCategories() {
		return this.subNotEventCategoryNames;
	}

	public Set<String> findEventCategories() {

		Set<String> noEventYAGOCategories = new HashSet<String>();
		noEventYAGOCategories.add("wordnet:causal_agent_100007347");
		noEventYAGOCategories.add("wordnet:matter_100020827");
		noEventYAGOCategories.add("wordnet:substance_114580597");
		noEventYAGOCategories.add("wordnet:object_100002684");
		noEventYAGOCategories.add("wordnet:thing_100002452");
		noEventYAGOCategories.add("wordnet:relation_100031921");
		noEventYAGOCategories.add("wordnet:measure_100033615");
		noEventYAGOCategories.add("wordnet:attribute_100024264");
		noEventYAGOCategories.add("wordnet:group_100031264");
		noEventYAGOCategories.add("wordnet:otherworld_105810143");
		noEventYAGOCategories.add("wordnet:set_107999699");
		noEventYAGOCategories.add("wordnet:communication_100033020");

		noEventYAGOCategories.add("wordnet:process_100029677");
		noEventYAGOCategories.add("wordnet:activity_100407535");
		noEventYAGOCategories.add("wordnet:speech_act_107160883");

		loadOntologyFromFile();

		Category eventCategory = categoriesByTypeAndTitle.get("wordnet").get("wordnet:event_100029378");
		Set<String> subEventCategoryNames = findSubCategoryNames(eventCategory);

		this.subNotEventCategoryNames = new HashSet<String>();
		for (String noEventCategoryName : noEventYAGOCategories) {
			Category noEventCategory = categoriesByTypeAndTitle.get("wordnet").get(noEventCategoryName);
			subNotEventCategoryNames.addAll(findSubCategoryNames(noEventCategory));
		}

		// TODO: Remove from subEventCategoryNames, but don't add to
		// subNotEventCategoryNames. In that case, categories are no negative
		// constraint, but also don't favor events.
		subEventCategoryNames.removeAll(subNotEventCategoryNames);

		return subEventCategoryNames;
	}

	private Set<String> findSubCategoryNames(Category eventCategory) {
		Set<Category> children = new HashSet<Category>();
		findAllChildren(eventCategory, children);

		Set<String> allCategoryNames = new HashSet<String>();

		// update YAGO by adding recent categories with years inside. If we find
		// at least three categories containing a year in [2010,2019], then
		// create category titles for all year in [2010,2019].

		Map<String, Integer> neutralYearStrings = new HashMap<String, Integer>();
		for (Category cat : children) {
			allCategoryNames.add(cat.getTitle());

			// replace the year with a standard representation ("dddd")
			String neutralYearString = cat.getTitle().replaceAll("201\\d", "dddd");

			if (!cat.getTitle().equals(neutralYearString)) {

				// avoid cases like "Domestic_cricket_competitions_in_dddd–15"
				if (neutralYearString.contains("dddd–"))
					continue;
				if (StringUtils.countMatches(neutralYearString, "dddd") > 1)
					continue;

				// count how often the standard representation appears
				if (!neutralYearStrings.containsKey(neutralYearString)) {
					neutralYearStrings.put(neutralYearString, 1);
				} else {
					neutralYearStrings.put(neutralYearString, neutralYearStrings.get(neutralYearString) + 1);
				}
			}
		}

		for (String neutralYearString : neutralYearStrings.keySet()) {
			int frequency = neutralYearStrings.get(neutralYearString);
			if (frequency > 3) {
				for (int year = 2010; year <= 2019; year++) {
					allCategoryNames.add(neutralYearString.replace("dddd", String.valueOf(year)));
				}
			}
		}

		return allCategoryNames;
	}

	private void findAllChildren(Category parent, Set<Category> children) {
		for (Category child : parent.getChildren()) {
			if (child.getType().equals("wikipedia"))
				children.add(child);
			findAllChildren(child, children);
		}
	}

	private void loadOntologyFromFile() {

		System.out.println("Load YAGO taxonomy from file.");

		String line;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_TAXONOMY);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) {
					continue;
				}

				String[] parts = line.split("\t");

				String property = parts[1];
				if (!property.equals("rdfs:subClassOf"))
					continue;

				// avoid line "<yagoNonConceptualWord> rdfs:subClassOf
				// xsd:string"
				if (!parts[0].contains("_"))
					continue;

				Category subject = createCategory(parts[0]);
				if (subject == null)
					continue;

				Category object = createCategory(parts[2]);
				if (object == null)
					continue;

				// if (subject.getTitle().equals("conflict")) {
				// System.out.println(line);
				// System.out.println(subject.getTitle());
				// System.out.println(object.getTitle());
				// System.out.println(object.getCompleteTitle());
				// }

				if (!categoriesByTypeAndTitle.containsKey(subject.getType()))
					categoriesByTypeAndTitle.put(subject.getType(), new HashMap<String, Category>());

				if (!categoriesByTypeAndTitle.containsKey(object.getType()))
					categoriesByTypeAndTitle.put(object.getType(), new HashMap<String, Category>());

				if (categoriesByTypeAndTitle.get(subject.getType()).containsKey(subject.getCompleteTitle())) {
					subject = categoriesByTypeAndTitle.get(subject.getType()).get(subject.getCompleteTitle());
				} else
					categoriesByTypeAndTitle.get(subject.getType()).put(subject.getCompleteTitle(), subject);

				if (categoriesByTypeAndTitle.get(object.getType()).containsKey(object.getCompleteTitle())) {
					object = categoriesByTypeAndTitle.get(object.getType()).get(object.getCompleteTitle());
				} else
					categoriesByTypeAndTitle.get(object.getType()).put(object.getCompleteTitle(), object);

				subject.addParent(object);
				object.addChild(subject);
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

		// Category exampleCategory =
		// categoriesByTypeAndTitle.get("wikipedia").get("wikipedia:Conflicts_in_1939");
		// Category exampleCategory =
		// categoriesByTypeAndTitle.get("wikipedia").get("wikipedia:Family_of_Bill_and_Hillary_Clinton");
		// Category exampleCategory = categoriesByTypeAndTitle.get("wikipedia")
		// .get("wikipedia:Presidents_of_the_United_States");

		// for (Category exampleCategory :
		// categoriesByTypeAndTitle.get("wikipedia").values())
		// printCategoryTree(exampleCategory, "");

		System.out.println("\tDone: Load YAGO taxonomy from file.");
	}

	private Category createCategory(String categoryString) {

		Category category = null;

		if (categoryString.contains("_")) {
			String categoryType = categoryString.substring(1, categoryString.indexOf("_"));
			if (categoryType.equals("wordnet")) {
				String completeTitle = categoryString.substring(categoryString.indexOf("_"));
				completeTitle = completeTitle.substring(1, completeTitle.lastIndexOf(">"));
				String title = completeTitle.substring(0, completeTitle.lastIndexOf("_"));
				category = new YAGOCategory(title, completeTitle);
				category.setType("wordnet");
			} else if (categoryType.equals("wikicat")) {
				String title = categoryString.substring(categoryString.indexOf("_") + 1);
				title = title.substring(0, title.length() - 1);
				category = new WikipediaCategory(title);
			} else {
				// never happens
				return null;
			}
		} else {

			String title = categoryString;

			if (title.startsWith("<")) {
				title = title.substring(1, title.lastIndexOf(">"));
			} else {
				// remove " ." at the end
				title = title.substring(0, title.length() - 2);
			}

			String type = "";

			if (title.contains(":")) {
				type = title.substring(0, title.indexOf(":"));
				title = title.substring(title.indexOf(":") + 1);
			}

			category = new YAGOCategory(title, title);
			category.setType(type);

			if (type.isEmpty())
				return null;
		}
		return category;
	}

}
