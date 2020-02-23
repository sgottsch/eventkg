package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class LabelsAndDescriptionsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public LabelsAndDescriptionsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("LabelsAndDescriptionsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect labels and descriptions of entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		extractRelations();
	}

	private void extractRelations() {

		System.out.println("Collect aliases");
		for (Language language : this.languages) {
			collectAliasesFromFile(FileLoader.getFile(FileName.WIKIDATA_ALIASES, language), language,
					DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		}

		System.out.println("Collect descriptions");
		for (Language language : this.languages) {
			collectDescriptionsFromFile(FileLoader.getFile(FileName.WIKIDATA_DESCRIPTIONS, language), language,
					DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA));
		}
	}

	private void collectDescriptionsFromFile(File file, Language language, DataSet dataSet) {

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(Config.TAB);
				String wikidataId = parts[0];

				Event event = allEventPagesDataSet.getEventByWikidataId(wikidataId);

				if (event != null) {
					event.addDescription(dataSet, language, parts[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void collectAliasesFromFile(File file, Language language, DataSet dataSet) {

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(Config.TAB);
				String wikidataId = parts[0];

				Event event = allEventPagesDataSet.getEventByWikidataId(wikidataId);

				if (event != null) {

					boolean first = true;
					for (String alias : parts) {
						if (first) {
							first = false;
							continue;
						}
						event.addAlias(dataSet, language, alias);
					}
				} else {
					Entity entity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(wikidataId);
					if (entity == null)
						continue;

					boolean first = true;
					for (String alias : parts) {
						if (first) {
							first = false;
							continue;
						}
						entity.addAlias(dataSet, language, alias);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
