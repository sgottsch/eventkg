package de.l3s.eventkg.source.wikipedia.model;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class LinksToCount {

	private Entity linkSource;

	private Entity linkTarget;

	private int count;

	private Language language;

	public LinksToCount(Entity linkSource, Entity linkTarget, int count, Language language) {
		super();
		this.linkSource = linkSource;
		this.linkTarget = linkTarget;
		this.count = count;
		this.language = language;
	}

	public Entity getEvent() {
		return linkSource;
	}

	public Entity getEntity() {
		return linkTarget;
	}

	public int getCount() {
		return count;
	}

	public Language getLanguage() {
		return language;
	}

	public GenericRelation toGenericRelation() {
		// we only store links where at least one event is involved
		return new GenericRelation(linkSource, DataSets.getInstance().getDataSet(this.language, Source.WIKIPEDIA),
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "links", linkTarget, (double) count,
				false);
	}

}
