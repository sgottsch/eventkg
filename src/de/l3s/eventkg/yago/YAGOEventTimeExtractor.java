package de.l3s.eventkg.yago;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Config.TimeSymbol;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class YAGOEventTimeExtractor extends Extractor {

	private PrintWriter resultsWriter;

	public static void main(String[] args) {
		Config.init("config_eventkb_local.txt");

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);
		languages.add(Language.FR);
		languages.add(Language.RU);
		languages.add(Language.PT);

		YAGOEventTimeExtractor e = new YAGOEventTimeExtractor(languages);
		e.run();
	}

	public YAGOEventTimeExtractor(List<Language> languages) {
		super("YAGOEventTimeExtractor", Source.YAGO, "?", languages);
	}

	public void run() {

		Map<String, TimeSymbol> targetProperties = loadTargetProperties();
		BufferedReader br = null;

		try {
			this.resultsWriter = FileLoader.getWriter(FileName.YAGO_EVENT_TIMES);

			br = FileLoader.getReader(FileName.YAGO_DATE_FACTS);

			String line;
			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("#") || line.startsWith("@"))
					continue;
				String[] parts = line.split(Config.TAB);

				if (targetProperties.containsKey(parts[1])) {
					resultsWriter.write(parts[0] + Config.TAB + parts[1] + Config.TAB + parts[2] + Config.TAB
							+ targetProperties.get(parts[1]).getTimeSymbol() + Config.NL);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				this.resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private Map<String, TimeSymbol> loadTargetProperties() {
		Map<String, TimeSymbol> targetProperties = new HashMap<String, TimeSymbol>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.YAGO_TIME_PROPERTIES);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);
				String id = parts[0];

				TimeSymbol propType = TimeSymbol.fromString(parts[1]);

				targetProperties.put(id, propType);
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

		return targetProperties;
	}

}
