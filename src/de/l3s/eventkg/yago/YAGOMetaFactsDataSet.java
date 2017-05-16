package de.l3s.eventkg.yago;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.yago.model.YAGOMetaFact;

public class YAGOMetaFactsDataSet {

	public static Map<String, Set<YAGOMetaFact>> loadMetaFacts(boolean temporalMetaFactsOnly) {

		Map<String, Set<YAGOMetaFact>> metaFacts = new HashMap<String, Set<YAGOMetaFact>>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_META_FACTS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("@") || line.startsWith("#"))
					continue;

				String[] parts = line.split("\t");

				// String factId = parts[0].substring(1, parts[0].length() - 1);
				String factId = parts[0];

				String property = parts[1];// .substring(1, parts[1].length() -
											// 1);

				String object = parts[2].substring(1, parts[2].length() - 2);

				YAGOMetaFact metaFact = new YAGOMetaFact(property, object);

				if (!temporalMetaFactsOnly || metaFact.isTemporal()) {
					if (!metaFacts.containsKey(factId))
						metaFacts.put(factId, new HashSet<YAGOMetaFact>());
					metaFacts.get(factId).add(metaFact);
				}

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
		return metaFacts;
	}

}
