package de.l3s.eventkg.yago;

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
public class YAGOEventLocationsExtractor extends Extractor {

	public YAGOEventLocationsExtractor(List<Language> languages) {
		super("YAGOEventLocationsExtractor", Source.YAGO,
				"Loads all facts with locations. Results are written to one file.", languages);
	}

	public void run() {
		System.out.println("Collect event pages.");
		extractTriples();
	}

	private void extractTriples() {

		System.out.println("Start writing to files.");
		PrintWriter locationsWriter = null;
		BufferedReader br = null;

		try {
			locationsWriter = FileLoader.getWriter(FileName.YAGO_LOCATIONS);

			br = FileLoader.getReader(FileName.YAGO_FACTS);

			String line;

			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("@") || line.startsWith("#"))
					continue;

				String[] parts = line.split(Config.TAB);

				String wikipediaLabel1 = parts[0].substring(1, parts[0].length() - 1);
				String wikipediaLabel2 = parts[2].substring(1, parts[2].length() - 3);
				String property = parts[1];

				// a) Locations
				if (property.equals("<isLocatedIn>") || property.equals("<happenedIn>")) {
					locationsWriter
							.write(wikipediaLabel1 + Config.TAB + property + Config.TAB + wikipediaLabel2 + Config.NL);
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				locationsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
