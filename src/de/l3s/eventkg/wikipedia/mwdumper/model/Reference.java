package de.l3s.eventkg.wikipedia.mwdumper.model;

public class Reference {

	private int startPosition;
	private int originalStartPosition;

	private String url;
	private String title;
	private String source;
	private String publicationDate;
	private String type;
	private String wholeInformationString;

	public int getStartPosition() {
		return this.startPosition;
	}

	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getPublicationDate() {
		return this.publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getOriginalStartPosition() {
		return this.originalStartPosition;
	}

	public void setOriginalStartPosition(int originalStartPosition) {
		this.originalStartPosition = originalStartPosition;
	}

	public String getWholeInformationString() {
		return this.wholeInformationString;
	}

	public void setWholeInformationString(String wholeInformationString) {
		this.wholeInformationString = wholeInformationString;
	}
}
