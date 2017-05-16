package de.l3s.eventkg.pipeline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Config {

	public static final String TAB = "\t";
	public static final String NL = "\n";
	public static final String SEP = " ";

	private static Map<String, String> properties;

	public static String getValue(String propertyName) {
		return properties.get(propertyName);
	}

	public static void init(String fileName) {
		properties = new HashMap<String, String>();

		try {
			// URI uri = Config.class.getResource(fileName).toURI();
			for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
				String[] parts = line.split("\t");
				properties.put(parts[0], parts[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
