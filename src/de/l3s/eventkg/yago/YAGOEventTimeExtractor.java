package de.l3s.eventkg.yago;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
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

		Set<String> targetProperties = loadTargetProperties();
		BufferedReader br = null;

		try {
			this.resultsWriter = FileLoader.getWriter(FileName.YAGO_EVENT_TIMES);

			br = FileLoader.getReader(FileName.YAGO_DATE_FACTS);

			String line;
			while ((line = br.readLine()) != null) {

				if (line.isEmpty() || line.startsWith("#") || line.startsWith("@"))
					continue;
				String[] parts = line.split(Config.TAB);

				if (targetProperties.contains(parts[1])) {
					resultsWriter.write(parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\n");
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

	private Set<String> loadTargetProperties() {
		Set<String> targetProperties = new HashSet<String>();
		targetProperties.add("<happenedOnDate>");
		targetProperties.add("<startedOnDate>");
		targetProperties.add("<endedOnDate>");
		return targetProperties;
	}

}
