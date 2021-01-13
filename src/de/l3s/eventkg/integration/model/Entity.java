package de.l3s.eventkg.integration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.integration.model.relation.Alias;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;

public class Entity {

	protected Map<Language, String> wikipediaLabels = new HashMap<Language, String>();
	protected Map<Language, Set<String>> wikidataLabels = new HashMap<Language, Set<String>>();

	private String wikidataId;
	private String yagoId;
	private Integer numericWikidataId;
	private String temporaryId;

	private String eventKGId;

	private boolean isEvent = false;

	private Set<Entity> subLocations = new HashSet<Entity>();

	private Set<Entity> parentLocations = new HashSet<Entity>();
	private Set<Entity> allParentLocations = new HashSet<Entity>();

	private Map<DateWithGranularity, Set<DataSet>> startTimesWithDataSets = new HashMap<DateWithGranularity, Set<DataSet>>();
	private Map<DateWithGranularity, Set<DataSet>> endTimesWithDataSets = new HashMap<DateWithGranularity, Set<DataSet>>();

	private Map<Position, DataSet> positionsWithDataSets = new HashMap<Position, DataSet>();

	private Set<Alias> aliases = new HashSet<Alias>();

	private boolean isLocation = false;
	private int isLocationCount = 0;
	private int isNoLocationCount = 0;

	private boolean isActor = false;
	private boolean isTextEvent;

	private Map<Entity, Map<Language, Integer>> linkCounts = null;

	public Entity() {

	}

	public Entity(int wikidataId) {
		this.numericWikidataId = wikidataId;
		this.wikidataId = "Q" + this.numericWikidataId;
	}

	public Entity(String wikidataId) {
		super();
		this.wikidataId = wikidataId;
		if (wikidataId != null)
			this.numericWikidataId = Integer.parseInt(wikidataId.substring(1));
	}

	public Entity(Language language, String wikipediaLabel) {
		super();
		this.wikipediaLabels.put(language, wikipediaLabel);
	}

	public Entity(Language language, String wikipediaLabel, String wikidataId) {
		super();
		this.wikipediaLabels.put(language, wikipediaLabel);
		this.wikidataId = wikidataId;
	}

	public Entity(Language language, String wikipediaLabel, String wikidataId, int numericWikidataId) {
		super();
		this.wikipediaLabels.put(language, wikipediaLabel);
		this.wikidataId = wikidataId;
		this.numericWikidataId = numericWikidataId;
	}

	public String getWikipediaLabel(Language language) {
		return wikipediaLabels.get(language);
	}

	public void addWikipediaLabel(Language language, String wikipediaLabel) {
		this.wikipediaLabels.put(language, wikipediaLabel);
	}

	public Set<String> getWikidataLabels(Language language) {
		return wikidataLabels.get(language);
	}

	public void addWikidataLabel(Language language, String wikidataLabel) {
		if (!this.wikidataLabels.containsKey(language))
			this.wikidataLabels.put(language, new HashSet<String>());
		this.wikidataLabels.get(language).add(wikidataLabel);
	}

	public String getWikidataId() {
		return wikidataId;
	}

	public void setWikidataId(String wikidataId) {
		this.wikidataId = wikidataId;
	}

	public String getYagoId() {
		return yagoId;
	}

	public void setYagoId(String yagoId) {
		this.yagoId = yagoId;
	}

	public boolean isEvent() {
		return isEvent;
	}

	public boolean isTextEvent() {
		return isTextEvent;
	}

	public void setTextEvent(boolean isTextEvent) {
		this.isTextEvent = isTextEvent;
	}

	public void setEvent(boolean isEvent) {
		this.isEvent = isEvent;
	}

	public String getWikipediaLabelsString(List<Language> languages) {
		List<String> labels = new ArrayList<String>();
		for (Language language : languages) {
			if (this.wikipediaLabels.containsKey(language))
				labels.add(language.getLanguageLowerCase() + ":" + this.wikipediaLabels.get(language));
			else
				labels.add(language.getLanguageLowerCase() + ":-");
		}
		return StringUtils.join(labels, " ");
	}

	public Map<Language, String> getWikipediaLabels() {
		return wikipediaLabels;
	}

	public Map<Language, Set<String>> getWikidataLabels() {
		return wikidataLabels;
	}

	public Set<Entity> getSubLocations() {
		return subLocations;
	}

	public void addSubLocation(Entity subLocation) {
		this.subLocations.add(subLocation);
	}

	public Set<Entity> getParentLocations() {
		return parentLocations;
	}

	public void addParentLocation(Entity parentLocation) {
		this.parentLocations.add(parentLocation);
	}

	public boolean isLocation() {
		return isLocation;
	}

	public void setLocation(boolean isLocation) {
		this.isLocation = isLocation;
	}

	public Set<Entity> getAllParentLocations() {
		return allParentLocations;
	}

	public void addAllParentLocation(Entity allParentLocation) {
		this.allParentLocations.add(allParentLocation);
	}

	public void addStartTime(DateWithGranularity startTime, DataSet dataSet) {
		if (!this.startTimesWithDataSets.containsKey(startTime))
			startTimesWithDataSets.put(startTime, new HashSet<DataSet>());
		this.startTimesWithDataSets.get(startTime).add(dataSet);
	}

	public void addEndTime(DateWithGranularity endTime, DataSet dataSet) {
		if (!this.endTimesWithDataSets.containsKey(endTime))
			endTimesWithDataSets.put(endTime, new HashSet<DataSet>());
		this.endTimesWithDataSets.get(endTime).add(dataSet);
	}

	public Map<DateWithGranularity, Set<DataSet>> getStartTimesWithDataSets() {
		return startTimesWithDataSets;
	}

	public Map<DateWithGranularity, Set<DataSet>> getEndTimesWithDataSets() {
		return endTimesWithDataSets;
	}

	public Integer getNumericWikidataId() {
		return numericWikidataId;
	}

	public void setNumericWikidataId(int numericWikidataId) {
		this.numericWikidataId = numericWikidataId;
	}

	public String getId() {
		return eventKGId;
	}

	public void setId(String outputId) {
		this.eventKGId = outputId;
	}

	public boolean isActor() {
		return isActor;
	}

	public void setActor(boolean isActor) {
		this.isActor = isActor;
	}

	public void addPosition(Position position, DataSet dataSet) {
		this.positionsWithDataSets.put(position, dataSet);
	}

	public Map<Position, DataSet> getPositionsWithDataSets() {
		return positionsWithDataSets;
	}

	public String getTemporaryId() {
		return temporaryId;
	}

	public void setTemporaryId(String temporaryId) {
		this.temporaryId = temporaryId;
	}

	public int getLocationCount() {
		return isLocationCount;
	}

	public void increaseIsLocationCount() {
		this.isLocationCount += 1;
	}

	public int getNoLocationCount() {
		return isNoLocationCount;
	}

	public void increaseIsNoLocationCount() {
		this.isNoLocationCount += 1;
	}

	public Set<Alias> getAliases() {
		return aliases;
	}

	public void addAlias(DataSet dataSet, Language language, String description) {
		this.aliases.add(new Alias(dataSet, description, language));
	}

	public void clearTimes() {
		this.startTimesWithDataSets = null;
		this.endTimesWithDataSets = null;
	}

	public void clearLocations() {
		this.subLocations = null;
		this.parentLocations = null;
	}

	public void clearPositions() {
		this.positionsWithDataSets = null;
	}

	public Set<Position> getPositionsOfDataSet(DataSet dataSet) {
		Set<Position> positionsOfDataSet = new HashSet<Position>();

		for (Position position : getPositionsWithDataSets().keySet()) {
			if (getPositionsWithDataSets().get(position) == dataSet)
				positionsOfDataSet.add(position);
		}

		return positionsOfDataSet;
	}

	public void addLinkCount(Entity targetEntity, Language language, int count) {

		if (this.linkCounts == null)
			this.linkCounts = new HashMap<Entity, Map<Language, Integer>>();

		if (!this.linkCounts.containsKey(targetEntity))
			this.linkCounts.put(targetEntity, new HashMap<Language, Integer>());

		this.linkCounts.get(targetEntity).put(language, count);
	}

	public void increaseLinkCount(Entity targetEntity, Language language) {

		if (this.linkCounts == null)
			this.linkCounts = new HashMap<Entity, Map<Language, Integer>>();

		if (!this.linkCounts.containsKey(targetEntity))
			this.linkCounts.put(targetEntity, new HashMap<Language, Integer>());

		if (!this.linkCounts.get(targetEntity).containsKey(language))
			this.linkCounts.get(targetEntity).put(language, 1);
		else
			this.linkCounts.get(targetEntity).put(language, this.linkCounts.get(targetEntity).get(language) + 1);

	}

	public Map<Entity, Map<Language, Integer>> getLinkCounts() {
		return linkCounts;
	}

	public void clearLinkCounts() {
		this.linkCounts = null;
	}

}
