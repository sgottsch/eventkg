package de.l3s.eventkg.source.wikipedia.model;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class LinkSetCount {

	private Entity entity1;

	private Entity entity2;

	private int count;

	private Language language;

	public LinkSetCount(Entity entity1, Entity entity2, int count, Language language) {
		super();
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.count = count;
		this.language = language;
	}

	public Entity getEvent() {
		return entity1;
	}

	public Entity getEntity() {
		return entity2;
	}

	public int getCount() {
		return count;
	}

	public Language getLanguage() {
		return language;
	}

	public GenericRelation toGenericRelation() {
		return new GenericRelation(entity1, DataSets.getInstance().getDataSet(this.language, Source.WIKIPEDIA),
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "mentions", entity2, (double) count,
				false);
	}

	public GenericRelation toGenericRelationSubjectObjectReverted() {
		return new GenericRelation(entity2, DataSets.getInstance().getDataSet(this.language, Source.WIKIPEDIA),
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "mentions", entity1, (double) count,
				false);
	}

}
