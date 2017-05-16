package de.l3s.eventkg.wikidata.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.wikidata.wdtk.datamodel.helpers.DataFormatter;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class TemporalPropertiesExtractor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";

	private int itemsWithTemporalPropertyCount = 0;

	private int itemCount = 0;

	private PrintStream out;

	private Set<String> temporalPropertyIds;

	public TemporalPropertiesExtractor() throws IOException {
		// open files for writing results
		out = FileLoader.getPrintStream(FileName.WIKIDATA_TEMPORAL_PROPERTIES);
		initProperties();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		for (String propertyId : temporalPropertyIds) {
			if (itemDocument.hasStatement(propertyId)) {

				StatementGroup statements = itemDocument.findStatementGroup(propertyId);

				if (statements != null) {
					for (Statement statement : statements) {

						if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
								&& statement.getClaim().getMainSnak().getValue() != null) {

							String timeString = transformToTimeString(
									(TimeValue) statement.getClaim().getMainSnak().getValue());

							if (timeString != null) {
								this.itemsWithTemporalPropertyCount++;
								out.print(itemDocument.getItemId().getId());
								out.print(TAB);
								out.print(propertyId);
								out.print(TAB);
								out.print(csvEscape(timeString));
								out.println();
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

	private String transformToTimeString(TimeValue value) {
		// return value.getYear() + "-" + value.getMonth() + "-" +
		// value.getDay() + "-" + value.getHour() + "-"
		// + value.getMinute() + "-" + value.getSecond();
		return DataFormatter.formatTimeISO8601(value);
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
		System.out.println("Found " + this.itemsWithTemporalPropertyCount + " matching items after scanning "
				+ this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.out.close();
	}

	public static FileOutputStream openExampleFileOuputStream(String filename) throws IOException {
		return new FileOutputStream(filename);
	}

	@Override
	public void open() {
	}

	private void initProperties() {

		// All time properties:
		// SELECT ?property ?propertyLabel
		// WHERE {
		// ?property a wikibase:Property .
		// ?property ?l wd:Q18636219
		// SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
		// }

		this.temporalPropertyIds = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");
				String id = parts[0];
				temporalPropertyIds.add(id);

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

}
