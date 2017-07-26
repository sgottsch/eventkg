package de.l3s.eventkg.wikidata.processors;

import java.io.IOException;
import java.io.PrintStream;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

/**
 * Given the complete Wikidata dump, extracts properties into three files:
 * <item1> <instanceOf> <item2> <br>
 * <item1> <subClassOf> <item2> <br>
 * <item1> <partOf> <item2>
 */
public class EventSubClassProcessor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";

	static final String instanceOfPropertyId = "P31";
	static final String subClassPropertyId = "P279";
	static final String partOfPropertyId = "P361";
	static final String followsPropertyId = "P155";
	static final String followedByPropertyId = "P156";

	int itemsWithInstanceOfCount = 0;
	int itemsWithSubClassCount = 0;
	int itemsWithPartOfCount = 0;
	int itemsWithFollowsCount = 0;
	int itemsWithFollowedByCount = 0;

	int itemCount = 0;

	PrintStream outInstanceOf;
	PrintStream outSubClass;
	PrintStream outPartOf;
	PrintStream outFollows;
	PrintStream outFollowedBy;

	public EventSubClassProcessor() throws IOException {
		// open files for writing results
		outInstanceOf = FileLoader.getPrintStream(FileName.WIKIDATA_INSTANCE_OF);
		outSubClass = FileLoader.getPrintStream(FileName.WIKIDATA_SUBCLASS_OF);
		outPartOf = FileLoader.getPrintStream(FileName.WIKIDATA_PART_OF);
		outFollows = FileLoader.getPrintStream(FileName.WIKIDATA_FOLLOWS);
		outFollowedBy = FileLoader.getPrintStream(FileName.WIKIDATA_FOLLOWED_BY);
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		// if (this.itemCount % 100000 == 0) {
		// printStatus();
		// }
		//
		// if (itemDocument.getItemId().getId().equals("Q209"))
		// System.out.println("EXAMPLE: " + itemDocument.getItemId().getId());
		// else
		// return;

		if (itemDocument.hasStatement(instanceOfPropertyId)) {

			StatementGroup statements = itemDocument.findStatementGroup(instanceOfPropertyId);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						if (id != null) {
							this.itemsWithInstanceOfCount++;
							outInstanceOf.print(itemDocument.getItemId().getId());
							outInstanceOf.print(TAB);
							outInstanceOf.print(csvEscape(itemDocument.findLabel("en")));
							outInstanceOf.print(TAB);
							outInstanceOf.print(csvEscape(id));
							outInstanceOf.print(TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outInstanceOf.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outInstanceOf.print("\\N");
							}
							outInstanceOf.println();
						}
					}
				}
			}
		}

		if (itemDocument.hasStatement(subClassPropertyId)) {

			StatementGroup statements = itemDocument.findStatementGroup(subClassPropertyId);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						if (id != null) {
							this.itemsWithSubClassCount++;
							outSubClass.print(itemDocument.getItemId().getId());
							outSubClass.print(TAB);
							outSubClass.print(csvEscape(itemDocument.findLabel("en")));
							outSubClass.print(TAB);
							outSubClass.print(csvEscape(id));
							outSubClass.print(TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outSubClass.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outSubClass.print("\\N");
							}
							outSubClass.println();
						}
					}
				}
			}
		}

		if (itemDocument.hasStatement(partOfPropertyId)) {

			StatementGroup statements = itemDocument.findStatementGroup(partOfPropertyId);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						if (id != null) {
							this.itemsWithPartOfCount++;
							outPartOf.print(itemDocument.getItemId().getId());
							outPartOf.print(TAB);
							outPartOf.print(csvEscape(itemDocument.findLabel("en")));
							outPartOf.print(TAB);
							outPartOf.print(csvEscape(id));
							outPartOf.print(TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outPartOf.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outPartOf.print("\\N");
							}
							outPartOf.println();
						}
					}
				}
			}
		}

		if (itemDocument.hasStatement(followsPropertyId)) {

			StatementGroup statements = itemDocument.findStatementGroup(followsPropertyId);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						if (id != null) {
							this.itemsWithFollowsCount++;
							outFollows.print(itemDocument.getItemId().getId());
							outFollows.print(TAB);
							outFollows.print(csvEscape(itemDocument.findLabel("en")));
							outFollows.print(TAB);
							outFollows.print(csvEscape(id));
							outFollows.print(TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outFollows.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outFollows.print("\\N");
							}
							outFollows.println();
						}
					}
				}
			}
		}

		if (itemDocument.hasStatement(followedByPropertyId)) {

			StatementGroup statements = itemDocument.findStatementGroup(followedByPropertyId);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						if (id != null) {
							this.itemsWithFollowedByCount++;
							outFollowedBy.print(itemDocument.getItemId().getId());
							outFollowedBy.print(TAB);
							outFollowedBy.print(csvEscape(itemDocument.findLabel("en")));
							outFollowedBy.print(TAB);
							outFollowedBy.print(csvEscape(id));
							outFollowedBy.print(TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outFollowedBy.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outFollowedBy.print("\\N");
							}
							outFollowedBy.println();
						}
					}
				}
			}
		}

		// Print progress every 100,000 items:
		if (this.itemCount % 100000 == 0) {
			printStatus();
		}
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// Nothing to do
	}

	/**
	 * Escapes a string for use in CSV. In particular, the string is quoted and
	 * quotation marks are escaped.
	 *
	 * @param string
	 *            the string to escape
	 * @return the escaped string
	 */
	private String csvEscape(String string) {
		if (string == null)
			return "\\N";
		else
			return string.replaceAll("\t", "   ");
		// if (string == null) {
		// return "\"\"";
		// } else {
		// return "\"" + string.replace("\"", "\"\"") + "\"";
		// }
	}

	/**
	 * Prints the current status, time and entity count.
	 */
	public void printStatus() {
		System.out.println("InstanceOf: Found " + this.itemsWithInstanceOfCount + " matching items after scanning "
				+ this.itemCount + " items.");
		System.out.println("Subclasses: Found " + this.itemsWithSubClassCount + " matching items after scanning "
				+ this.itemCount + " items.");
		System.out.println("Part of: Found " + this.itemsWithPartOfCount + " matching items after scanning "
				+ this.itemCount + " items.");
		System.out.println("Follows: Found " + this.itemsWithFollowsCount + " matching items after scanning "
				+ this.itemCount + " items.");
		System.out.println("Followed by: Found " + this.itemsWithFollowedByCount + " matching items after scanning "
				+ this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.outInstanceOf.close();
		this.outSubClass.close();
		this.outPartOf.close();
		this.outFollows.close();
		this.outFollowedBy.close();
	}

	@Override
	public void open() {
	}

}
