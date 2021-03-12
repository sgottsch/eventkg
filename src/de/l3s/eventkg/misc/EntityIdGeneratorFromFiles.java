package de.l3s.eventkg.misc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class EntityIdGeneratorFromFiles {

	private Map<String, String> eventKGToWikidata = new HashMap<String, String>();

	public EntityIdGeneratorFromFiles(String fileName) {
		System.out.println("Init Entity ID Generator.");
		loadWikidata(fileName);
		System.out.println("Finished.");
	}

	public String getWikidataIdByEventKGId(String eventKGId) {
		return eventKGToWikidata.get(eventKGId);
	}

	public void loadWikidata(String fileName) {

		System.out.println("loadWikidata");

		LineIterator it = null;
		try {
			it = FileUtils.lineIterator(new File(fileName), "UTF-8");
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(" ");
				String wikidataId = parts[1];
				String oekgId = parts[0];
				this.eventKGToWikidata.put(oekgId, wikidataId);
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

	}

}
