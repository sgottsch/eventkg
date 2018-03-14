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
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RelationsToEventPagesProcessor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";

	private int itemsWithEventCount = 0;
	private int itemsWithEntityRelationCount = 0;

	private int itemCount = 0;

	private PrintStream outEventRelations;
	private PrintStream outEntityRelations;

	private Set<String> targetEventIds;
	private Set<String> entitiesWithExistenceTimes;

	private Set<String> forbiddenPropertyIds;
	private Set<String> temporalPropertyIds;

	public RelationsToEventPagesProcessor(AllEventPagesDataSet allEventPagesDataSet) {
		this.targetEventIds = allEventPagesDataSet.getWikidataIdsOfAllEvents();
		this.entitiesWithExistenceTimes = allEventPagesDataSet.getWikidataIdsOfEntitiesWithExistenceTime();

		try {
			outEventRelations = FileLoader.getPrintStream(FileName.WIKIDATA_EVENT_RELATIONS);
			outEntityRelations = FileLoader.getPrintStream(FileName.WIKIDATA_ENTITY_RELATIONS);
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

		// ignore properties like
		// P856 official website (not used)
		// P361 part of (already covered)
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

		boolean tc = false;
		// if (itemDocument.getItemId().getId().equals("Q567") ||
		// itemDocument.getItemId().getId().equals("567"))
		// tc = true;

		if (tc) {
			System.out.println("Test case: " + itemDocument.getItemId().getId());
		}

		boolean subjectIsEvent = this.targetEventIds.contains(itemDocument.getItemId().getId());
		boolean subjectHasExistenceTime = this.entitiesWithExistenceTimes.contains(itemDocument.getItemId().getId());

		if (tc) {
			System.out.println("subjectIsEvent: " + subjectIsEvent);
			System.out.println("subjectHasExistenceTime: " + subjectHasExistenceTime);
		}

		for (StatementGroup statementGroup : itemDocument.getStatementGroups()) {
			String propertyId = statementGroup.getProperty().getId();
			if (!this.forbiddenPropertyIds.contains(propertyId)) {
				// property okay

				if (tc) {
					System.out.println("propertyId: " + propertyId);
				}

				for (Statement statement : statementGroup) {

					if (statement.getClaim() != null && statement.getClaim().getMainSnak() != null
							&& statement.getClaim().getMainSnak().getValue() != null) {

						String id = null;
						try {
							id = ((ItemIdValue) statement.getClaim().getMainSnak().getValue()).getId();

							boolean objectIsEvent = this.targetEventIds.contains(id);
							boolean objectHasExistenceTime = this.entitiesWithExistenceTimes.contains(id);

							if (tc) {
								System.out.println("\nid: " + id);
								System.out.println("objectIsEvent: " + objectIsEvent);
								System.out.println("objectHasExistenceTime: " + objectHasExistenceTime);
							}

							if (subjectIsEvent || objectIsEvent) {

								if (tc) {
									System.out.println("subjectIsEvent || objectIsEvent");
								}

								itemsWithEventCount += 1;

								outEventRelations.print(statement.getStatementId() + "\t"
										+ itemDocument.getItemId().getId() + "\t" + propertyId + "\t" + id + "\n");
							} else if (subjectHasExistenceTime && objectHasExistenceTime) {

								if (tc) {
									System.out.println("subjectHasExistenceTime && objectHasExistenceTime");
								}

								itemsWithEntityRelationCount += 1;

								outEntityRelations.print(statement.getStatementId() + "\t"
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

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		// Nothing to do
	}

	public void printStatus() {
		System.out.println("Found " + this.itemsWithEventCount + " event relations and "
				+ this.itemsWithEntityRelationCount + " entity relations after scanning " + this.itemCount + " items.");
	}

	public void close() {
		printStatus();
		this.outEventRelations.close();
		this.outEntityRelations.close();
	}

	@Override
	public void open() {
	}

}
