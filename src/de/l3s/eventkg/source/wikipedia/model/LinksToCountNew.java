package de.l3s.eventkg.source.wikipedia.model;

import java.util.HashMap;
import java.util.Map;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.DataSet;

public class LinksToCountNew {

	private Entity linkSource;

	private Entity linkTarget;

	private Map<DataSet, Integer> counts;

	private boolean involvesEvent;

	public LinksToCountNew(Entity linkSource, Entity linkTarget, boolean involvesEvent) {
		super();
		this.linkSource = linkSource;
		this.linkTarget = linkTarget;
		this.involvesEvent = involvesEvent;
		this.counts = new HashMap<DataSet, Integer>();
	}

	public Entity getSource() {
		return linkSource;
	}

	public Entity getTarget() {
		return linkTarget;
	}

	public boolean involvesEvent() {
		return involvesEvent;
	}

	public void setInvolvesEvent(boolean involvesEvent) {
		this.involvesEvent = involvesEvent;
	}

	public void addCount(DataSet dataSet, int count) {
		this.counts.put(dataSet, count);
	}

	public Map<DataSet, Integer> getCounts() {
		return counts;
	}

}
