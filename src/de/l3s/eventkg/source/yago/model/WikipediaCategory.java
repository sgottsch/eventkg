package de.l3s.eventkg.source.yago.model;

import java.util.HashSet;
import java.util.Set;

public class WikipediaCategory implements Category {

	private String title;
	private String type;

	private Set<Category> parents = new HashSet<Category>();
	private Set<String> parentTitles = new HashSet<String>();

	private Set<Category> children = new HashSet<Category>();

	public WikipediaCategory(String title) {
		this.title = title;
		this.type = "wikipedia";
	}

	public WikipediaCategory(String title, String parentTitle) {
		this.title = title;
		if (parentTitle != null)
			this.parentTitles.add(parentTitle);
		this.type = "wikipedia";
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<Category> getParents() {
		return parents;
	}

	public Set<Category> getChildren() {
		return children;
	}

	public void setChildren(Set<Category> children) {
		this.children = children;
	}

	public Set<String> getParentTitles() {
		return parentTitles;
	}

	public void addChild(Category child) {
		this.children.add(child);
	}

	public void addParentTitle(String parentCategoryTitle) {
		this.parentTitles.add(parentCategoryTitle);
	}

	public void addParent(Category parentCategory) {
		this.parents.add(parentCategory);
	}

	@Override
	public String getCompleteTitle() {
		return type + ":" + title;
	}

}
