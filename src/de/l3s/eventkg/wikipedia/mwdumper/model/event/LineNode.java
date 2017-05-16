package de.l3s.eventkg.wikipedia.mwdumper.model.event;

import java.util.ArrayList;
import java.util.List;

public class LineNode {

	private String line;
	private List<LineNode> children;
	private LineNode parent;
	private int level;
	private NodeType type;
	private PartialDate partialDate;
	private String originalLine;
	private String titles;

	public LineNode(String line, int level) {
		this.line = line;
		this.originalLine = line;
		this.level = level;
		this.children = new ArrayList<LineNode>();
		this.partialDate = new PartialDate();
	}

	public String getLine() {
		return this.line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public List<LineNode> getChildren() {
		return this.children;
	}

	public void setChildren(List<LineNode> children) {
		this.children = children;
	}

	public LineNode getParent() {
		return this.parent;
	}

	public void setParent(LineNode parent) {
		this.parent = parent;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public LineNode getParentAtLevel(int level) {
		LineNode parent = this.parent;
		while (parent != null) {
			if (parent.getLevel() == level) {
				return parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public void addChild(LineNode child) {
		this.children.add(child);
		child.setParent(this);
	}

	public void print() {
		print("");
	}

	private void print(String indent) {
		if (this.line != null) {
			System.out.println(indent + this.partialDate + ": " + this.line);
		} else
			System.out.println(indent + this.partialDate);
		for (LineNode child : this.children)
			child.print(indent + "\t");
	}

	public NodeType getType() {
		return this.type;
	}

	public void setType(NodeType type) {
		this.type = type;
	}

	public static enum NodeType {
		DATE, TITLE, EVENT, LINE;
	}

	public PartialDate getPartialDate() {
		return this.partialDate;
	}

	public void setPartialDate(PartialDate partialDate) {
		this.partialDate = partialDate;
	}

	public String getOriginalLine() {
		return this.originalLine;
	}

	public void setOriginalLine(String originalLine) {
		this.originalLine = originalLine;
	}

	public String getTitles() {
		return this.titles;
	}

	public void setTitles(String titles) {
		this.titles = titles;
	}
}