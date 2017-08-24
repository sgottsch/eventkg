package de.l3s.eventkg.source.yago.model;

import java.util.HashSet;
import java.util.Set;

public class YAGOCategory implements Category {

	private String title;

	private String type;

	private String completeTitle;

	private Set<Category> parents = new HashSet<Category>();
	private Set<Category> children = new HashSet<Category>();

	public YAGOCategory(String title, String completeTitle) {
		super();
		this.title = title;
		this.completeTitle = completeTitle;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	public String getCompleteTitle() {
		return type + ":" + completeTitle;
	}

	public void setCompleteTitle(String completeTitle) {
		this.completeTitle = completeTitle;
	}

	public Set<Category> getChildren() {
		return children;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void addParent(Category parentCategory) {
		this.parents.add(parentCategory);
	}

	public void addChild(Category child) {
		this.children.add(child);
	}

	@Override
	public Set<Category> getParents() {
		return this.parents;
	}

}
