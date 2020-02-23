package de.l3s.eventkg.source.wikidata.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.source.wikidata.WikidataSnakType;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import edu.stanford.nlp.util.StringUtils;

public class RelationsToEventPagesProcessor implements EntityDocumentDumpProcessor {

	public static final String TAB = "\t";
	public static final String TAB2 = "|";

	private int itemsWithEventCount = 0;
	private int itemsWithEntityRelationCount = 0;

	private int itemCount = 0;

	private PrintStream outEventRelations;
	private PrintStream outEventLiteralRelations;
	private PrintStream outEntityRelations;

	private Set<Integer> targetEventIds;
	private Set<Integer> entitiesWithExistenceTimes;

	private Set<String> forbiddenPropertyIds;
	private Set<String> temporalPropertyIds;

	public RelationsToEventPagesProcessor(AllEventPagesDataSet allEventPagesDataSet) {
		this.targetEventIds = allEventPagesDataSet.getWikidataIdsOfAllEvents();
		this.entitiesWithExistenceTimes = allEventPagesDataSet.getWikidataIdsOfEntitiesWithExistenceTime();

		try {
			outEventRelations = FileLoader.getPrintStream(FileName.WIKIDATA_EVENT_RELATIONS);
			outEventLiteralRelations = FileLoader.getPrintStream(FileName.WIKIDATA_EVENT_LITERALS_RELATIONS);
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

		if (fileName.getFileName().toLowerCase().endsWith("json")) {
			try {
				JSONObject wikidataJSON = new JSONObject(FileLoader.readFile(fileName));
				JSONArray bindings = wikidataJSON.getJSONObject("results").getJSONArray("bindings");
				for (int i = 0; i < bindings.length(); i++) {
					JSONObject binding = bindings.getJSONObject(i);

					JSONObject property1JSON = binding.getJSONObject("property");
					String property1 = property1JSON.getString("value");
					property1 = property1.substring(property1.lastIndexOf("/") + 1);

					propertyIds.add(property1);
				}

			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}

		} else {

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

		boolean subjectIsEvent = this.targetEventIds.contains(getNumericId(itemDocument.getItemId().getId()));
		boolean subjectHasExistenceTime = this.entitiesWithExistenceTimes
				.contains(getNumericId(itemDocument.getItemId().getId()));

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

							boolean objectIsEvent = this.targetEventIds.contains(getNumericId(id));
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

							// literals

							if (subjectIsEvent) {

								String res = statement.getClaim().getMainSnak().getValue()
										.accept(new ValueVisitor<String>() {

											@Override
											public String visit(DatatypeIdValue val) {
												return null;
											}

											@Override
											public String visit(EntityIdValue val) {
												return null;
											}

											@Override
											public String visit(GlobeCoordinatesValue val) {
												return WikidataSnakType.GLOBE_COORDINATE.toString() + TAB
														+ val.getLatitude() + TAB2 + val.getLongitude() + TAB2
														+ val.getPrecision() + TAB2 + val.getGlobe();
											}

											@Override
											public String visit(MonolingualTextValue val) {
												return WikidataSnakType.MONOLINGUAL_TEXT.toString() + TAB
														+ tabify(val.getText()) + TAB2 + val.getLanguageCode();
											}

											@Override
											public String visit(QuantityValue val) {
												return WikidataSnakType.QUANTITY.toString() + TAB
														+ val.getNumericValue() + TAB2 + val.getLowerBound() + TAB2
														+ val.getUpperBound() + TAB2 + val.getUnit();
											}

											@Override
											public String visit(StringValue val) {

												// ignore images (file formats
												// see here:
												// https://www.wikidata.org/wiki/Property:P18)
												String stringValue = val.getString().toLowerCase();
												if (stringValue.contains(".") && (stringValue.endsWith(".png")
														|| stringValue.endsWith(".svg") || stringValue.endsWith(".jpg")
														|| stringValue.endsWith(".jpeg") || stringValue.endsWith(".jpe")
														|| stringValue.endsWith(".tif") || stringValue.endsWith(".tiff")
														|| stringValue.endsWith(".gif") || stringValue.endsWith(".xcf")
														|| stringValue.endsWith(".pdf") || stringValue.endsWith(".djvu")
														|| stringValue.endsWith(".webp")))
													return null;

												return WikidataSnakType.STRING.toString() + TAB
														+ tabify(val.getString());
											}

											@Override
											public String visit(TimeValue val) {
												return WikidataSnakType.TIME.toString() + TAB + val.getYear() + TAB2
														+ val.getMonth() + TAB2 + val.getDay() + TAB2 + val.getMinute()
														+ TAB2 + val.getSecond() + TAB2 + val.getPrecision();
											}

										});

								if (res == null)
									continue;

								Set<String> refStrings = new LinkedHashSet<String>();

								// collect the temporal validity of the literal
								// fact. Unfortunately, it is sometimes stored
								// as
								// qualifier, sometimes as reference.

								for (Reference reference : statement.getReferences()) {
									for (Iterator<Snak> it = reference.getAllSnaks(); it.hasNext();) {
										Snak snak = it.next();
										if (this.temporalPropertyIds.contains(snak.getPropertyId().getId())) {
											if (snak.getValue() == null)
												continue;
											refStrings.add(
													snak.getPropertyId().getId() + " " + snak.getValue().toString());
										}
									}
								}

								for (Iterator<Snak> it = statement.getClaim().getAllQualifiers(); it.hasNext();) {
									Snak snak = it.next();
									if (this.temporalPropertyIds.contains(snak.getPropertyId().getId())) {
										if (snak.getValue() == null)
											continue;
										refStrings.add(snak.getPropertyId().getId() + " " + snak.getValue().toString());
									}
								}

								this.outEventLiteralRelations.print(itemDocument.getItemId().getId() + TAB + propertyId
										+ TAB + res + TAB + statement.getRank() + TAB
										+ StringUtils.join(refStrings, TAB2) + "\n");
							}

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

	private static int getNumericId(String id) {
		return Integer.valueOf(id.substring(1));
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
		this.outEventLiteralRelations.close();
		this.outEntityRelations.close();
	}

	@Override
	public void open() {
	}

	private String tabify(String text) {
		if (text.contains(TAB2))
			return null;
		text = text.replace(TAB, "   ");
		return text;
	}

}
