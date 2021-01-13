package de.l3s.eventkg.integration.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.WikidataIdMappings;
import de.l3s.eventkg.integration.integrator.PositionsIntegrator;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Position;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.pipeline.output.RDFWriterName;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class PositionsCollector extends Extractor {

	private List<Language> languages;

	private WikidataIdMappings wikidataIdMappings;

	private TriplesWriter dataStoreWriter;

	public PositionsCollector(List<Language> languages, WikidataIdMappings wikidataIdMappings,
			TriplesWriter dataStoreWriter) {
		super("PositionsCollector", Source.ALL, "Collects and fuses event positions into a common graph.", languages);
		this.languages = languages;
		this.dataStoreWriter = dataStoreWriter;
		this.wikidataIdMappings = wikidataIdMappings;
	}

	public void run() {
		loadPositions();
		integratePositions();
		clear();
	}

	private void clear() {
		this.dataStoreWriter.resetNumberOfInstances(RDFWriterName.EVENT_BASE_RELATIONS);
		for (Entity entity : this.wikidataIdMappings.getEntitiesByWikidataNumericIds().values()) {
			entity.clearPositions();
		}
	}

	private void integratePositions() {
		PositionsIntegrator positionsIntegrator = new PositionsIntegrator(languages, dataStoreWriter,
				wikidataIdMappings);
		positionsIntegrator.run();
	}

	private void loadPositions() {
		BufferedReader br = null;

		try {
			br = FileLoader.getReader(FileName.ALL_POSITIONS);

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				Entity entity = this.wikidataIdMappings.getEntityByWikidataId(parts[0]);
				if (entity == null) {
					continue;
				}

				String dataSetId = parts[4];
				DataSet dataSet = DataSets.getInstance().getDataSetById(dataSetId);

				entity.addPosition(new Position(Double.valueOf(parts[2]), Double.valueOf(parts[3])), dataSet);

				// GenericRelation relation = new GenericRelation(event2,
				// dataSet,
				// PrefixList.getInstance().getPrefix(PrefixEnum.SEM),
				// "hasSubEvent", event1, null, false);
				// DataStore.getInstance().addGenericRelation(relation);

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
