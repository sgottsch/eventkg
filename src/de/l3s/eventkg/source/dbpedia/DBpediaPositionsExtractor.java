package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaPositionsExtractor extends Extractor {

	private PrintWriter resultsWriter;

	public DBpediaPositionsExtractor(List<Language> languages) {
		super("DBpediaPositionsExtractor", Source.DBPEDIA,
				"Loads all DBpedia relations where a subject is connected with a position via <http://www.georss.org/georss/point>.",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> targetProperties = loadPositionProperties();

		Set<String> foundEvents = new HashSet<String>();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_POSITIONS, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {

			resultsWriter.write("subject" + Config.TAB + "latitude" + Config.TAB + "longitude" + Config.NL);

			if (FileLoader.fileExists(FileName.DBPEDIA_MAPPINGS_LITERALS, language)) {

				try {
					br = FileLoader.getReader(FileName.DBPEDIA_GEO_COORDINATES, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("#"))
						continue;

					String[] parts = line.split(" ");
					String property = parts[1];

					// if (!targetEvents.contains(object) ||
					// !targetEvents.contains(subject))
					// continue;

					if (targetProperties.contains(property)) {

						String subject = parts[0];
						if (!subject.contains("resource"))
							continue;

						String longitude = parts[2];
						longitude = StringUtils.strip(longitude, "\"");

						String latitude = parts[3];
						latitude = StringUtils.strip(latitude, "\"");

						// Date date =
						// TimeTransformer.generateEarliestTimeFromXsd(timeString);

						subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));

						String fileLine = subject + Config.TAB + longitude + Config.TAB + latitude;

						if (foundEvents.contains(fileLine))
							continue;
						resultsWriter.write(fileLine + "\n");
						foundEvents.add(fileLine);
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static Set<String> loadPositionProperties() {

		// Extract this from a meta file

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("<http://www.georss.org/georss/point>");
		// targetProperties.add("<http://www.w3.org/2003/01/geo/wgs84_pos#lat>");
		// targetProperties.add("<http://www.w3.org/2003/01/geo/wgs84_pos#long>");

		return targetProperties;
	}

}
