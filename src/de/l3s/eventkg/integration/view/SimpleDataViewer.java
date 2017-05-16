package de.l3s.eventkg.integration.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class SimpleDataViewer {

	public static void main(String[] args) {

		Config.init("config_eventkb_local.txt");

		String wikidataId = "Q130847"; // Battle_of_Verdun
		// String wikidataId = "Q362"; // Zweiter_Weltkrieg

		System.out.println("Start.");

		Set<FileName> fileNames = new HashSet<FileName>();
		for (FileName fileName : FileName.values()) {
			if (fileName == FileName.ALL_LINK_SETS)
				continue;
			if (fileName == FileName.ALL_LINKED_BY_COUNTS)
				continue;
//			if (fileName == FileName.ALL_LINK_COUNTS)
//				continue;
//			if (fileName == FileName.ALL_TEMPORAL_RELATIONS)
//				continue;
			if (fileName.isResultsData() && fileName.getSource() == Source.ALL) {
				fileNames.add(fileName);
			}
		}

		for (FileName fileName : fileNames) {
			processFile2(fileName, wikidataId);
		}
	}

	private static void processFile(FileName fileName, String wikidataId) {
		System.out.println("\tprocessFile: " + fileName);
		try {
			String content = FileLoader.readFile(new File(FileLoader.getPath(fileName)));

			if (content.isEmpty())
				return;

			for (String line : content.split(Config.NL)) {
				String[] parts = line.split(Config.TAB);

				if (parts[0].equals(wikidataId))
					System.out.println(fileName + ": " + line);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void processFile2(FileName fileName, String wikidataId) {
		System.out.println("\tprocessFile: " + fileName);

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split(Config.TAB);

				if (parts[0].equals(wikidataId))
					System.out.println(fileName + ": " + line);
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

	}

}
