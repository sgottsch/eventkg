package de.l3s.eventkg.source.wikidata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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

public class WikidataMorePartOfRelationsFromFileFinder extends Extractor {

	public static void main(String[] args) {
		Config.init(args[0]);
		List<Language> ls = new ArrayList<Language>();
		ls.add(Language.EN);
		WikidataMorePartOfRelationsFromFileFinder ff = new WikidataMorePartOfRelationsFromFileFinder(ls);
		ff.run();
	}

	public WikidataMorePartOfRelationsFromFileFinder(List<Language> languages) {
		super("WikidataMorePartOfRelationsFromFileFinder", Source.WIKIDATA,
				"Given a manual set of relations such as \"sports season of league or competition\", creates a file with additional \"part of\" event relations.",
				languages);
	}

	public void run() {
		findMorePartOfRelations();
	}

	private void findMorePartOfRelations() {

		Set<String> targetProperties = new HashSet<String>();
		targetProperties.add(WikidataResource.PROPERTY_SPORTS_SEASON_OF_LEAGUE_OR_COMPETITION.getId());
		targetProperties.add(WikidataResource.PROPERTY_SEASON_OF_CLUB_OR_TEAM.getId());
		targetProperties.add(WikidataResource.PROPERTY_PART_OF_THE_SERIES.getId());

		PrintWriter writer = null;

		BufferedReader br = null;
		try {
			writer = FileLoader.getWriter(FileName.WIKIDATA_PART_OF_SERIES);
			try {
				br = FileLoader.getReader(FileName.WIKIDATA_EVENT_RELATIONS);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				if (targetProperties.contains(parts[2])) {
					writer.write(parts[1] + "\t" + parts[2] + "\t" + parts[3] + "\n");
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
			writer.close();
		}

	}

}
