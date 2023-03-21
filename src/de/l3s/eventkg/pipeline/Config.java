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

	public static final String SUB_LOCATION_SYMBOL = "s";
	public static final String PARENT_LOCATION_SYMBOL = "p";

	public enum TimeSymbol {

		START_TIME("s"),
		END_TIME("e"),
		NO_TIME("n"),
		START_AND_END_TIME("b"); // both

		private String timeSymbol;

		TimeSymbol(String timeSymbol) {
			this.timeSymbol = timeSymbol;
		}

		public String getTimeSymbol() {
			return timeSymbol;
		}

		public static TimeSymbol fromString(String symbolString) {
			for (TimeSymbol symbol : TimeSymbol.values()) {
				if (symbol.getTimeSymbol().equals(symbolString)) {
					return symbol;
				}
			}
			return null;
		}
	}

	private static Map<String, String> properties;

	public static String getValue(String propertyName) {
		return properties.get(propertyName);
	}

	public static void init(String fileName) {
		
		System.out.println("Initialise config file: " + fileName);

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

	public static String getResourceURI() {
		return getURL() + "resource/";
	}

	public static String getSchemaURI() {
		return getURL() + "schema/";
	}

	public static String getGraphURI() {
		return getURL() + "graph/";
	}

	public static String getGraphURI(String name) {
		return "<" + getURL() + "graph/" + name + ">";
	}

	public static String getResourceURI(String name) {
		return "<" + getURL() + "resource/" + name + ">";
	}

	public static String getURL() {
		return getValue("url");
	}

}
