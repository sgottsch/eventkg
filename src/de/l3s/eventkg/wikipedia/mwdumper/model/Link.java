package de.l3s.eventkg.wikipedia.mwdumper.model;

public class Link {

	private String name;
	private int start;
	private int end;
	private int pageId;
	private String type;
	private String anchorText;
	private String nameWithoutHash;
	private String hashSuffix;

	public Link(String name, int start, int end) {
		this.name = name;
		this.start = start;
		this.end = end;

		if (!name.contains("#") || name.equals("#")) {
			this.nameWithoutHash = name;
			this.hashSuffix = null;
		} else {
			String[] parts = name.split("#");
			this.nameWithoutHash = parts[0];
			if (parts.length == 1)
				this.hashSuffix = null;
			else
				this.hashSuffix = parts[1];
		}

	}

	public int getStart() {
		return this.start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return this.end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPageId() {
		return this.pageId;
	}

	public void setPageId(int pageId) {
		this.pageId = pageId;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAnchorText() {
		return this.anchorText;
	}

	public void setAnchorText(String anchorText) {
		this.anchorText = anchorText;
	}

	public String getNameWithoutHash() {
		return this.nameWithoutHash;
	}

	public String getHashSuffix() {
		return this.hashSuffix;
	}
}
