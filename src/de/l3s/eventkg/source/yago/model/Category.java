package de.l3s.eventkg.source.yago.model;

import java.util.Set;

public interface Category {

	public String getTitle();

	public String getType();

	public void setType(String type);

	public void addParent(Category category);

	public void addChild(Category child);

	public Set<Category> getParents();

	public String getCompleteTitle();
	
	public Set<Category> getChildren();

}
