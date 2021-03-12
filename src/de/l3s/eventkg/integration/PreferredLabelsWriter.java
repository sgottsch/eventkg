package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.pipeline.output.RDFWriterName;
import de.l3s.eventkg.pipeline.output.TriplesWriter;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class PreferredLabelsWriter extends Extractor {

	private Map<String, Integer> ranksPerId = new HashMap<String, Integer>();
	private Map<String, String> labelsPerId = new HashMap<String, String>();

	private TriplesWriter triplesWriter;

	public PreferredLabelsWriter(List<Language> languages, TriplesWriter triplesWriter) {
		super("PreferredLabelsWriter", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Collect preferred of events and entities.", languages);
		this.triplesWriter = triplesWriter;
	}

	public void run() {
		writePreferredLabels();
	}

	public void writePreferredLabels() {

		System.out.println("writePreferredLabels");

		Map<String, Integer> rankedGraphs = new HashMap<String, Integer>();

		List<Language> rankedWikipediaLanguages = Arrays.asList(Language.EN, Language.DE, Language.FR, Language.IT,
				Language.ES, Language.PT);

		List<Language> allRankedWikipediaLanguages = new ArrayList<Language>();
		allRankedWikipediaLanguages.addAll(rankedWikipediaLanguages);

		for (Language language : Language.values()) {
			if (!allRankedWikipediaLanguages.contains(language))
				allRankedWikipediaLanguages.add(language);
		}

		// put languages with non-Latin letters at the end
		List<Language> languagesWithNonLatinLetters = Arrays.asList(Language.ZH, Language.RU, Language.JA, Language.BG);
		for (Language language : languagesWithNonLatinLetters) {
			allRankedWikipediaLanguages.remove(language);
			allRankedWikipediaLanguages.add(language);
		}

		String wikidataGraph = TriplesWriter.createResourceWithPrefix(
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_GRAPH),
				DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA).getId());

		int rank = 0;
		for (Language language : allRankedWikipediaLanguages) {
			DataSet dataSet = DataSets.getInstance().getDataSet(language, Source.WIKIPEDIA);
			if (dataSet != null) {
				String graph = TriplesWriter.createResourceWithPrefix(
						PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_GRAPH), dataSet.getId());
				rankedGraphs.put(graph, rank);
				rank += 1;
			}
			String wikidataWithLanguageGraph = wikidataGraph.replace("wikidata", "wikidata_" + language.getLanguage());
			rankedGraphs.put(wikidataWithLanguageGraph, rank);
			rank += 1;
		}

		for (String graph : rankedGraphs.keySet()) {
			System.out.println(graph + " -> " + rankedGraphs.get(graph));
		}

		List<FileName> files = new ArrayList<FileName>();
		files.add(FileName.ALL_TTL_EVENTS);
		files.add(FileName.ALL_TTL_ENTITIES);

		for (FileName file : files) {
			System.out.println("File: " + file.getFileName());
			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(file);
				while (it.hasNext()) {
					String line = it.nextLine();

					if (!line.contains("http://www.w3.org/2000/01/rdf-schema#label"))
						continue;

					String id = line.substring(0, line.indexOf(" "));

					String rest = line.substring(line.indexOf("/rdf-schema#label>") + 19);
					rest = rest.substring(0, rest.lastIndexOf(" "));

					String label = rest.substring(0, rest.lastIndexOf(" "));
					String currentGraph = rest.substring(rest.lastIndexOf(" ")).trim();

					int index = label.lastIndexOf("@");
					String languageString = label.substring(index + 1);
					label = label.substring(1, index - 1);

					currentGraph = currentGraph.replace("wikidata", "wikidata_" + languageString);

					Integer currentRank = rankedGraphs.get(currentGraph);

					if (currentRank == null)
						continue;

					label = "\"" + label + "\"@" + languageString;

					if (!ranksPerId.containsKey(id) || ranksPerId.get(id) > currentRank) {
						labelsPerId.put(id, label);
						ranksPerId.put(id, currentRank);
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
			ranksPerId = new HashMap<String, Integer>();
			writeOutput();
			labelsPerId = new HashMap<String, String>();
		}

	}

	private void writeOutput() {

		System.out.println("Number of labels: " + this.labelsPerId.keySet().size());

		triplesWriter.resetNumberOfInstances(RDFWriterName.PREFERRED_LABELS);

		String graph = TriplesWriter.createResourceWithPrefix(
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_GRAPH),
				DataSets.getInstance().getDataSetWithoutLanguage(Source.EVENT_KG).getId());

		for (Iterator<String> it = this.labelsPerId.keySet().iterator(); it.hasNext();) {
			String id = it.next();
			triplesWriter.startInstance();
			triplesWriter.writePreferredLabel(id, this.labelsPerId.get(id), graph);
			triplesWriter.endInstance();
			it.remove();
		}

	}

}
