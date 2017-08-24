package de.l3s.eventkg.source.wikipedia.mwdumper.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Paragraph {

	private String text;
	private String title;
	private String completeTitle;

	private Paragraph topParagraph;
	private List<Paragraph> subParagraphs;

	private int level;
	private int id;
	private Integer textParagraphId;
	private Integer startPosition;

	private Date firstDate;
	private Date lastDate;

	private List<Reference> references;
	private List<Link> links;

	public Paragraph(String text, String title, int id) {
		this.text = text;
		this.title = title;
		this.completeTitle = title;
		this.id = id;
		this.subParagraphs = new ArrayList<Paragraph>();
		this.links = new ArrayList<Link>();
		this.references = new ArrayList<Reference>();
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<Paragraph> getSubParagraphs() {
		return this.subParagraphs;
	}

	public void setSubParagraphs(List<Paragraph> subParagraphs) {
		this.subParagraphs = subParagraphs;
	}

	public void addSubParagraph(Paragraph subParagraph) {
		this.subParagraphs.add(subParagraph);
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Paragraph getTopParagraph() {
		return this.topParagraph;
	}

	public void setTopParagraph(Paragraph topParagraph) {
		this.topParagraph = topParagraph;
		this.completeTitle = String.valueOf(topParagraph.getCompleteTitle()) + ">" + this.title.replaceAll(">", "~");
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public Paragraph getTopParagraphAtLevel(int level) throws NullPointerException {
		Paragraph currentTopParagraph = this;
		while (currentTopParagraph.getLevel() != level) {
			currentTopParagraph = currentTopParagraph.getTopParagraph();
		}
		return currentTopParagraph;
	}

	public List<Link> getLinks() {
		return this.links;
	}

	public void addLink(Link link) {
		this.links.add(link);
	}

	public List<Reference> getReferences() {
		return this.references;
	}

	public void addReference(Reference reference) {
		this.references.add(reference);
	}

	public void setReferences(List<Reference> references) {
		this.references = references;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCompleteTitle() {
		return this.completeTitle;
	}

	public Date getFirstDate() {
		return this.firstDate;
	}

	public void setFirstDate(Date firstDate) {
		this.firstDate = firstDate;
	}

	public Date getLastDate() {
		return this.lastDate;
	}

	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
	}

	public Integer getTextParagraphId() {
		return this.textParagraphId;
	}

	public void setTextParagraphId(Integer textParagraphId) {
		this.textParagraphId = textParagraphId;
	}

	public Integer getStartPosition() {
		return this.startPosition;
	}

	public void setStartPosition(Integer startPosition) {
		this.startPosition = startPosition;
	}
}
