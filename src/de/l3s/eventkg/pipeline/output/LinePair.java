package de.l3s.eventkg.pipeline.output;

public class LinePair {

	private String line;
	private String lineLight;

	public LinePair(String line, String lineLight) {
		super();
		this.line = line;
		this.lineLight = lineLight;
	}

	public String getLine() {
		return line;
	}

	public String getLineLight() {
		return lineLight;
	}

}
