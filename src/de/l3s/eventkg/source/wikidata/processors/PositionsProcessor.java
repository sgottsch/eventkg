package de.l3s.eventkg.source.wikidata.processors;

import java.io.IOException;
import java.io.PrintStream;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class PositionsProcessor implements EntityDocumentDumpProcessor {

	private int itemsWithPositionCount = 0;

	private int itemCount = 0;

	private PrintStream outPositions;

	private String positionProperty;

	private void loadPositionPropertyId() {
		this.positionProperty = "P625";
	}

	public PositionsProcessor() throws IOException {
		// open files for writing results
		outPositions = FileLoader.getPrintStream(FileName.WIKIDATA_POSITIONS);
		outPositions.print("subjectId" + Config.TAB + "subjectLabel" + Config.TAB + "latitude" + Config.TAB
				+ "longitude" + Config.TAB + "precision" + Config.TAB + "globe" + Config.TAB + "subjectWikiEnLabel");
		outPositions.println();
		loadPositionPropertyId();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		if (itemDocument.hasStatement(positionProperty)) {

			StatementGroup statements = itemDocument.findStatementGroup(positionProperty);

			if (statements != null) {
				for (Statement statement : statements) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						GlobeCoordinatesValue value = null;
						try {
							value = (GlobeCoordinatesValue) statement.getClaim().getMainSnak().getValue();
						} catch (ClassCastException e) {
							continue;
						}

						if (value != null) {
							this.itemsWithPositionCount++;
							String globe = value.getGlobe();
							globe = globe.substring(globe.lastIndexOf("/") + 1);
							outPositions.print(itemDocument.getItemId().getId());
							outPositions.print(Config.TAB);
							outPositions.print(csvEscape(itemDocument.findLabel("en")));
							outPositions.print(Config.TAB);
							outPositions.print(value.getLatitude());
							outPositions.print(Config.TAB);
							outPositions.print(value.getLongitude());
							outPositions.print(Config.TAB);
							outPositions.print(value.getPrecision());
							outPositions.print(Config.TAB);
							outPositions.print(globe);
							outPositions.print(Config.TAB);
							SiteLink enwiki = itemDocument.getSiteLinks().get("enwiki");
							if (enwiki != null) {
								outPositions.print(csvEscape(enwiki.getPageTitle()));
							} else {
								outPositions.print("\\N");
							}
							outPositions.println();
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
		System.out.println("Found " + this.itemsWithPositionCount + " items with positions after scanning "
				+ this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.outPositions.close();
	}

	@Override
	public void open() {
	}

}
