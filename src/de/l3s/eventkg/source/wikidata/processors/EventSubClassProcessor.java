package de.l3s.eventkg.source.wikidata.processors;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.source.wikidata.WikidataProperty;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

/**
 * Given the complete Wikidata dump, extracts properties into three files:
 * <item1> <instanceOf> <item2> <br>
 * <item1> <subClassOf> <item2> <br>
 * <item1> <partOf> <item2>
 */
public class EventSubClassProcessor implements EntityDocumentDumpProcessor {

	int itemsWithInstanceOfCount = 0;
	int itemsWithSubClassCount = 0;
	int itemsWithPartOfCount = 0;
	int itemsWithFollowsCount = 0;
	int itemsWithFollowedByCount = 0;

	int itemCount = 0;

	Set<WikidataProperty> partOfSeriesProperties = new HashSet<WikidataProperty>();

	PrintStream outInstanceOf;
	PrintStream outSubClass;
	PrintStream outPartOf;
	PrintStream outPartOfSeries;
	PrintStream outFollows;
	PrintStream outFollowedBy;

	public EventSubClassProcessor() throws IOException {
		// open files for writing results
		outInstanceOf = FileLoader.getPrintStream(FileName.WIKIDATA_INSTANCE_OF);
		outSubClass = FileLoader.getPrintStream(FileName.WIKIDATA_SUBCLASS_OF);
		outPartOf = FileLoader.getPrintStream(FileName.WIKIDATA_PART_OF);
		outPartOfSeries = FileLoader.getPrintStream(FileName.WIKIDATA_PART_OF_SERIES);
		outFollows = FileLoader.getPrintStream(FileName.WIKIDATA_FOLLOWS);
		outFollowedBy = FileLoader.getPrintStream(FileName.WIKIDATA_FOLLOWED_BY);

		partOfSeriesProperties.add(WikidataProperty.SPORTS_SEASON_OF_LEAGUE_OR_COMPETITION);
		partOfSeriesProperties.add(WikidataProperty.SEASON_OF_CLUB_OR_TEAM);
		partOfSeriesProperties.add(WikidataProperty.PART_OF_THE_SERIES);
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

		if (itemDocument.hasStatement(WikidataProperty.INSTANCE_OF.getId())) {

			StatementGroup statements = itemDocument.findStatementGroup(WikidataProperty.INSTANCE_OF.getId());

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

						if (id != null) {
							this.itemsWithInstanceOfCount++;
							outInstanceOf.print(itemDocument.getEntityId().getId());
							outInstanceOf.print(Config.TAB);
							outInstanceOf.print(csvEscape(itemDocument.findLabel("en")));
							outInstanceOf.print(Config.TAB);
							outInstanceOf.print(csvEscape(id));
							outInstanceOf.print(Config.TAB);
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

		if (itemDocument.hasStatement(WikidataProperty.SUB_CLASS.getId())) {

			StatementGroup statements = itemDocument.findStatementGroup(WikidataProperty.SUB_CLASS.getId());

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

						if (id != null) {
							this.itemsWithSubClassCount++;
							outSubClass.print(itemDocument.getEntityId().getId());
							outSubClass.print(Config.TAB);
							outSubClass.print(csvEscape(itemDocument.findLabel("en")));
							outSubClass.print(Config.TAB);
							outSubClass.print(csvEscape(id));
							outSubClass.print(Config.TAB);
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

		if (itemDocument.hasStatement(WikidataProperty.PART_OF.getId())) {

			StatementGroup statements = itemDocument.findStatementGroup(WikidataProperty.PART_OF.getId());

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

						if (id != null) {
							this.itemsWithPartOfCount++;
							outPartOf.print(itemDocument.getEntityId().getId());
							outPartOf.print(Config.TAB);
							outPartOf.print(csvEscape(itemDocument.findLabel("en")));
							outPartOf.print(Config.TAB);
							outPartOf.print(csvEscape(id));
							outPartOf.print(Config.TAB);
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

		for (WikidataProperty property : partOfSeriesProperties) {
			if (itemDocument.hasStatement(property.getId())) {

				StatementGroup statements = itemDocument.findStatementGroup(property.getId());

				if (statements != null) {
					for (Statement statement : statements) {

						if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
								&& statement.getClaim().getValue() != null) {

							String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

							if (id != null) {
								this.itemsWithPartOfCount++;
								outPartOfSeries.print(itemDocument.getEntityId().getId());
								outPartOfSeries.print(Config.TAB);
								outPartOfSeries.print(csvEscape(itemDocument.findLabel("en")));
								outPartOfSeries.print(Config.TAB);
								outPartOfSeries.print(csvEscape(id));
								outPartOfSeries.print(Config.TAB);
								SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
								if (enwiki != null) {
									outPartOfSeries.print(csvEscape(enwiki.getPageTitle()));
								} else {
									outPartOfSeries.print("\\N");
								}
								outPartOfSeries.print(Config.TAB);
								outPartOfSeries.print(property.getId());
								outPartOfSeries.println();
							}
						}
					}
				}
			}
		}

		if (itemDocument.hasStatement(WikidataProperty.FOLLOWS.getId())) {

			StatementGroup statements = itemDocument.findStatementGroup(WikidataProperty.FOLLOWS.getId());

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

						if (id != null) {
							this.itemsWithFollowsCount++;
							outFollows.print(itemDocument.getEntityId().getId());
							outFollows.print(Config.TAB);
							outFollows.print(csvEscape(itemDocument.findLabel("en")));
							outFollows.print(Config.TAB);
							outFollows.print(csvEscape(id));
							outFollows.print(Config.TAB);
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

		if (itemDocument.hasStatement(WikidataProperty.FOLLOWED_BY.getId())) {

			StatementGroup statements = itemDocument.findStatementGroup(WikidataProperty.FOLLOWED_BY.getId());

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getValue() != null) {

						String id = ((ItemIdValue) statement.getClaim().getValue()).getId();

						if (id != null) {
							this.itemsWithFollowedByCount++;
							outFollowedBy.print(itemDocument.getEntityId().getId());
							outFollowedBy.print(Config.TAB);
							outFollowedBy.print(csvEscape(itemDocument.findLabel("en")));
							outFollowedBy.print(Config.TAB);
							outFollowedBy.print(csvEscape(id));
							outFollowedBy.print(Config.TAB);
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
		this.outPartOfSeries.close();
		this.outFollows.close();
		this.outFollowedBy.close();
	}

	@Override
	public void open() {
	}

}
