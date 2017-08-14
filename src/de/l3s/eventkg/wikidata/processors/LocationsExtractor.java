package de.l3s.eventkg.wikidata.processors;

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

public class LocationsExtractor implements EntityDocumentDumpProcessor {

	private int itemsWithLocationCount = 0;

	private int itemCount = 0;

	private PrintStream outLocations;

	private Set<String> locationPropertyIds;

	private void loadLocationPropertyIds() {

		this.locationPropertyIds = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_LOCATION_PROPERTY_NAMES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");
				locationPropertyIds.add(parts[0]);

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

	public LocationsExtractor() throws IOException {
		// open files for writing results
		outLocations = FileLoader.getPrintStream(FileName.WIKIDATA_LOCATIONS);
		loadLocationPropertyIds();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		for (String locationPropertyId : locationPropertyIds) {

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
								this.itemsWithLocationCount++;
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
		System.out.println("Found " + this.itemsWithLocationCount + " matching items after scanning " + this.itemCount
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
