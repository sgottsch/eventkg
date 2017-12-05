package de.l3s.eventkg.source.wikidata.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.wikidata.wdtk.datamodel.helpers.DataFormatter;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class FactsWithTemporalSnaksProcessor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";

	private int itemsWithTemporalPropertiesCount = 0;

	private int itemCount = 0;

	private PrintStream outTemporalFacts;

	private Set<String> temporalPropertyIds;

	private void loadForbiddenPropertyIds() {
		this.temporalPropertyIds = new HashSet<String>();
		this.temporalPropertyIds.addAll(loadPropertyIds(FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME));
	}

	private Set<String> loadPropertyIds(FileName fileName) {

		Set<String> propertyIds = new HashSet<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");
				propertyIds.add(parts[0]);

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

		return propertyIds;
	}

	public FactsWithTemporalSnaksProcessor() throws IOException {
		// open files for writing results
		outTemporalFacts = FileLoader.getPrintStream(FileName.WIKIDATA_TEMPORAL_FACTS);
		loadForbiddenPropertyIds();
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		for (StatementGroup statementGroup : itemDocument.getStatementGroups()) {
			String propertyId = statementGroup.getProperty().getId();

			for (Statement statement : statementGroup) {

				if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
						&& statement.getClaim().getMainSnak().getValue() != null) {

					String id = null;
					try {
						id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

						List<Snak> temporalSnaks = new ArrayList<Snak>();
						// search for temporal qualifiers
						for (Iterator<Snak> it = statement.getClaim().getAllQualifiers(); it.hasNext();) {
							Snak snak = it.next();
							if (temporalPropertyIds.contains(snak.getPropertyId().getId())) {
								if (snak.getValue() != null)
									temporalSnaks.add(snak);
							}
						}

						if (!temporalSnaks.isEmpty()) {

							itemsWithTemporalPropertiesCount += 1;

							outTemporalFacts.print(statement.getStatementId() + "\t" + itemDocument.getItemId().getId()
									+ "\t" + propertyId + "\t" + id + "\n");
							for (Snak snak : temporalSnaks) {

								String timeString = transformToTimeString((TimeValue) snak.getValue());

								outTemporalFacts.print(
										"\t" + snak.getPropertyId().getId() + "\t" + csvEscape(timeString) + "\n");
							}
						}

					} catch (ClassCastException e) {
						continue;
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
		return value.getPrecision() + ";" + DataFormatter.formatTimeISO8601(value);
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
		// if (string == null) {
		// return "\"\"";
		// } else {
		// return "\"" + string.replace("\"", "\"\"") + "\"";
		// }
	}

	public void printStatus() {
		System.out.println("Found " + this.itemsWithTemporalPropertiesCount + " matching items after scanning "
				+ this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.outTemporalFacts.close();
	}

	@Override
	public void open() {
	}

}
