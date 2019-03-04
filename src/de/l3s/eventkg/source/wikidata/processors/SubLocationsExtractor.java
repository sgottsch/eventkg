package de.l3s.eventkg.source.wikidata.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class SubLocationsExtractor implements EntityDocumentDumpProcessor {

	private int itemsWithSubLocationCount = 0;
	private int itemsWithParentLocationCount = 0;

	private int itemCount = 0;

	private PrintStream outLocations;

	private Set<String> subLocationPropertyIds;
	private Set<String> parentLocationPropertyIds;

	private void loadLocationPropertyIds() {

		this.subLocationPropertyIds = new HashSet<String>();
		this.parentLocationPropertyIds = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_SUB_LOCATION_PROPERTY_NAMES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (parts[2].equals("s"))
					subLocationPropertyIds.add(parts[0]);
				else if (parts[2].equals("p"))
					parentLocationPropertyIds.add(parts[0]);

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

	}

	public SubLocationsExtractor() throws IOException {
		// open files for writing results
		outLocations = FileLoader.getPrintStream(FileName.WIKIDATA_SUB_LOCATIONS);
		outLocations.print("parent/sub" + Config.TAB + "subjectId" + Config.TAB + "subjectLabel" + Config.TAB
				+ "locationId" + Config.TAB + "propertyId" + Config.TAB + "subjectWikiEnLabel");
		outLocations.println();
		loadLocationPropertyIds();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		for (String locationPropertyId : subLocationPropertyIds) {

			if (itemDocument.hasStatement(locationPropertyId)) {

				StatementGroup statements = itemDocument.findStatementGroup(locationPropertyId);

				if (statements != null) {
					for (Statement statement : statements) {

						if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
								&& statement.getClaim().getMainSnak().getValue() != null) {

							String id = null;
							try {
								id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();
							} catch (ClassCastException e) {
								continue;
							}

							if (id != null) {
								this.itemsWithSubLocationCount++;
								outLocations.print(Config.SUB_LOCATION_SYMBOL);
								outLocations.print(Config.TAB);
								outLocations.print(itemDocument.getItemId().getId());
								outLocations.print(Config.TAB);
								outLocations.print(csvEscape(itemDocument.findLabel("en")));
								outLocations.print(Config.TAB);
								outLocations.print(csvEscape(id));
								outLocations.print(Config.TAB);
								outLocations.print(locationPropertyId);
								outLocations.print(Config.TAB);
								SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
								if (enwiki != null) {
									outLocations.print(csvEscape(enwiki.getPageTitle()));
								} else {
									outLocations.print("\\N");
								}
								outLocations.println();
							}
						}
					}
				}
			}
		}

		for (String locationPropertyId : parentLocationPropertyIds) {

			if (itemDocument.hasStatement(locationPropertyId)) {

				StatementGroup statements = itemDocument.findStatementGroup(locationPropertyId);

				if (statements != null) {
					for (Statement statement : statements) {

						if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
								&& statement.getClaim().getMainSnak().getValue() != null) {

							String id = null;
							try {
								id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();
							} catch (ClassCastException e) {
								continue;
							}

							if (id != null) {
								this.itemsWithParentLocationCount++;
								outLocations.print(Config.PARENT_LOCATION_SYMBOL);
								outLocations.print(Config.TAB);
								outLocations.print(itemDocument.getItemId().getId());
								outLocations.print(Config.TAB);
								outLocations.print(csvEscape(itemDocument.findLabel("en")));
								outLocations.print(Config.TAB);
								outLocations.print(csvEscape(id));
								outLocations.print(Config.TAB);
								outLocations.print(locationPropertyId);
								outLocations.print(Config.TAB);
								SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
								if (enwiki != null) {
									outLocations.print(csvEscape(enwiki.getPageTitle()));
								} else {
									outLocations.print("\\N");
								}
								outLocations.println();
							}
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

	private String csvEscape(String string) {
		if (string == null)
			return "\\N";
		else
			return string.replaceAll("\t", "   ");
	}

	public void printStatus() {
		System.out.println("Found " + this.itemsWithSubLocationCount + " sub location items and "
				+ this.itemsWithParentLocationCount + " parent location items after scanning " + this.itemCount
				+ " items.");
	}

	public void close() {
		printStatus();
		this.outLocations.close();
	}

	@Override
	public void open() {
	}

}
