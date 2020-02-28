package de.l3s.eventkg.source.dbpedia;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;
import org.apache.commons.text.StringEscapeUtils;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriter;
import de.l3s.eventkg.integration.DataStoreWriterMode;
import de.l3s.eventkg.integration.model.FileType;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaTypeLabelsExtractor extends Extractor {

	public DBpediaTypeLabelsExtractor(List<Language> languages) {
		super("DBpediaTypeLabelsExtractor", Source.DBPEDIA, "Loads all DBpedia:ontology type labels.", languages);
	}

	@Override
	public void run() {

		PrefixList prefixList = PrefixList.getInstance();

		FileType fileType = FileType.NQ;

		Set<String> dbpediaClasses = new HashSet<String>();
		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.DBPEDIA_ONTOLOGY);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(" ");
				if (parts[1].equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
						&& parts[2].equals("<http://www.w3.org/2002/07/owl#Class>")) {
					dbpediaClasses.add(parts[0]);
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

		PrintWriter writer = null;
		PrintWriter writerPreview = null;

		DataStoreWriter dataStoreWriter = new DataStoreWriter(languages,
				DataStoreWriterMode.USE_IDS_OF_CURRENT_EVENTKG_VERSION);
		dataStoreWriter.initPrefixes();

		int lineNo = 0;
		try {
			writer = FileLoader.getWriter(FileName.ALL_TTL_TYPE_LABELS_DBPEDIA);
			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_TYPE_LABELS_DBPEDIA_PREVIEW);

			LineIterator it2 = null;
			try {
				it2 = FileLoader.getLineIterator(FileName.DBPEDIA_ONTOLOGY);
				while (it2.hasNext()) {
					String line = it2.nextLine();
					line = StringEscapeUtils.unescapeJava(line);

					String[] parts = line.split(" ");
					if (parts[1].equals("<http://www.w3.org/2000/01/rdf-schema#label>")
							&& dbpediaClasses.contains(parts[0])) {

						String classId = parts[0];
						classId = classId.substring(classId.lastIndexOf("/") + 1, classId.length() - 1);
						String label = "";
						for (int i = 2; i < parts.length - 1; i++)
							label += parts[i] + " ";
						label = label.trim();

						String languageString = label.substring(label.indexOf("@") + 1);
						Language language = Language.getLanguageOrNull(languageString);

						if (language != null && this.languages.contains(language)) {
							label = label.substring(1, label.length() - 4);
							DataSet dataSet = DataSets.getInstance().getDataSet(language, Source.DBPEDIA);

							dataStoreWriter.writeTriple(writer, writerPreview, lineNo,
									prefixList.getPrefix(PrefixEnum.DBPEDIA_ONTOLOGY), classId,
									prefixList.getPrefix(PrefixEnum.RDFS), "label", null, label, true, dataSet,
									language, fileType);
							lineNo += 1;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it2.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
			writerPreview.close();
		}

	}

}
