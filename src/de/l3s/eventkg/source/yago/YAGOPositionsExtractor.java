package de.l3s.eventkg.source.yago;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

/**
 * Loads all facts with locations. Results are written to one file.
 */
public class YAGOPositionsExtractor extends Extractor {

	public YAGOPositionsExtractor(List<Language> languages) {
		super("YAGOPositionsLocationsExtractor", Source.YAGO,
				"Loads all YAGO facts where a subject is connected to a location via <hasLongitude> or <hasLatitude>.",
				languages);
	}

	public void run() {
		System.out.println("Collect YAGO latitude and longitude facts.");
		extractTriples();
	}

	private void extractTriples() {

		System.out.println("Start writing to files.");
		PrintWriter positionsWriter = null;
		BufferedReader br = null;

		try {
			positionsWriter = FileLoader.getWriter(FileName.YAGO_POSITIONS);

			br = FileLoader.getReader(FileName.YAGO_LITERAL_FACTS);

			positionsWriter.write("subject" + Config.TAB + "property" + Config.TAB + "object" + Config.NL);
			
			String line;

			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("@") || line.startsWith("#"))
					continue;

				String[] parts = line.split(Config.TAB);

				String wikipediaLabel1 = parts[0].substring(1, parts[0].length() - 1);
				String property = parts[1];

				// a) Locations
				if (property.equals("<hasLongitude>") || property.equals("<hasLatitude>")) {
					positionsWriter.write(wikipediaLabel1 + Config.TAB + property + Config.TAB + parts[2] + Config.NL);
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				positionsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
