package de.l3s.eventkg.source.wikidata;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class WikidataTypeLabelsExtractor extends Extractor {

	private Set<Integer> wikidataTypes;
	private TriplesWriter datastoreWriter;

	public WikidataTypeLabelsExtractor(List<Language> languages, Set<Integer> wikidataTypes,
			TriplesWriter dataStoreWriter) {
		super("WikidataTypeLabelsExtractor", Source.WIKIDATA, "Loads all Wikidata type labels.", languages);
		this.wikidataTypes = wikidataTypes;
		this.datastoreWriter = dataStoreWriter;
	}

	@Override
	public void run() {

		DataSet dataSet = DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA);

		// Set<Integer> nonWikidataTypeIds = new THashSet<Integer>();
		// Set<Integer> wikidataTypeIds = new THashSet<Integer>();
		//
		// System.out.println("Load classes from \"instance of\" file.");
		// LineIterator it = null;
		// try {
		// it = FileLoader.getLineIterator(FileName.WIKIDATA_INSTANCE_OF);
		// while (it.hasNext()) {
		// String line = it.nextLine();
		// String wikidataId = line.split("\t")[0];
		// nonWikidataTypeIds.add(Integer.valueOf(wikidataId.substring(1)));
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// try {
		// it.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		//
		// System.out.println("Load classes from \"sub class of\" file.");
		// LineIterator it2 = null;
		// try {
		// it2 = FileLoader.getLineIterator(FileName.WIKIDATA_SUBCLASS_OF);
		// while (it2.hasNext()) {
		// String line = it2.nextLine();
		// String wikidataId1 = line.split("\t")[2];
		// int numWikidataId1 = Integer.valueOf(wikidataId1.substring(1));
		// if (!nonWikidataTypeIds.contains(numWikidataId1))
		// wikidataTypeIds.add(Integer.valueOf(numWikidataId1));
		//
		// String wikidataId2 = line.split("\t")[0];
		// int numWikidataId2 = Integer.valueOf(wikidataId2.substring(1));
		// if (!nonWikidataTypeIds.contains(numWikidataId2))
		// wikidataTypeIds.add(Integer.valueOf(numWikidataId2));
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// } finally {
		// try {
		// it2.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }

		System.out.println("Write class labels.");
		for (Language language : this.languages) {

			LineIterator it3 = null;

			try {
				it3 = FileLoader.getLineIterator(FileName.WIKIDATA_LABELS, language);
				while (it3.hasNext()) {
					String line = it3.nextLine();
					String wikidataId = line.split("\t")[0];
					int numericWikidataId = Integer.valueOf(wikidataId.substring(1));

					if (wikidataTypes.contains(numericWikidataId)) {
						datastoreWriter.startInstance();
						datastoreWriter.writeWikidataTypeLabelTriple(wikidataId, line.split("\t")[1], dataSet, language,
								false);
						datastoreWriter.endInstance();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it3.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
