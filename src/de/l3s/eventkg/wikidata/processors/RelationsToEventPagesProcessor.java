package de.l3s.eventkg.wikidata.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.wikidata.wdtk.datamodel.helpers.DataFormatter;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RelationsToEventPagesProcessor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";

	private int itemsWithEventCount = 0;

	private int itemCount = 0;

	private PrintStream outEventRelations;

	private Set<String> targetEventIds;

	private Set<String> forbiddenPropertyIds;
	private Set<String> temporalPropertyIds;

	public RelationsToEventPagesProcessor(AllEventPagesDataSet allEventPagesDataSet) {
		this.targetEventIds = allEventPagesDataSet.getWikidataIdsOfAllEvents();
		try {
			outEventRelations = FileLoader.getPrintStream(FileName.WIKIDATA_EVENT_RELATIONS);
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadForbiddenPropertyIds();
	}

	private void loadForbiddenPropertyIds() {
		this.forbiddenPropertyIds = new HashSet<String>();
		this.forbiddenPropertyIds.addAll(loadPropertyIds(FileName.WIKIDATA_LOCATION_PROPERTY_NAMES));
		this.forbiddenPropertyIds.addAll(loadPropertyIds(FileName.WIKIDATA_EXTERNAL_IDS_PROPERTY_NAMES));
		this.forbiddenPropertyIds.addAll(loadPropertyIds(FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME));
		this.forbiddenPropertyIds.addAll(loadPropertyIds(FileName.WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES));
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

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		boolean subjectIsEvent = this.targetEventIds.contains(itemDocument.getItemId().getId());

		for (StatementGroup statementGroup : itemDocument.getStatementGroups()) {
			String propertyId = statementGroup.getProperty().getId();
			if (!this.forbiddenPropertyIds.contains(propertyId)) {
				// property okay

				for (Statement statement : statementGroup) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = null;
						try {
							id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

							boolean objectIsEvent = this.targetEventIds.contains(id);

							if (subjectIsEvent || objectIsEvent) {

								itemsWithEventCount += 1;

								outEventRelations.print(statement.getStatementId() + "\t"
										+ itemDocument.getItemId().getId() + "\t" + propertyId + "\t" + id + "\n");
							}

						} catch (ClassCastException e) {
							continue;
						}
					}
				}
			} else {
				continue;
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
		// if (string == null) {
		// return "\"\"";
		// } else {
		// return "\"" + string.replace("\"", "\"\"") + "\"";
		// }
	}

	public void printStatus() {
		System.out.println(
				"Found " + this.itemsWithEventCount + " matching items after scanning " + this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.outEventRelations.close();
	}

	@Override
	public void open() {
	}

}
