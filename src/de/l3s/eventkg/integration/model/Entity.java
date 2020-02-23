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

	private Set<DateWithGranularity> startTimes = new HashSet<DateWithGranularity>();;
	private Map<DateWithGranularity, Set<DataSet>> startTimesWithDataSets = new HashMap<DateWithGranularity, Set<DataSet>>();
	private Map<DataSet, DateWithGranularity> dataSetsWithStartTimes = new HashMap<DataSet, DateWithGranularity>();

	private Set<DateWithGranularity> endTimes = new HashSet<DateWithGranularity>();;
	private Map<DateWithGranularity, Set<DataSet>> endTimesWithDataSets = new HashMap<DateWithGranularity, Set<DataSet>>();
	private Map<DataSet, DateWithGranularity> dataSetsWithEndTimes = new HashMap<DataSet, DateWithGranularity>();

	private Set<Position> positions = new HashSet<Position>();
	private Map<Position, DataSet> positionsWithDataSets = new HashMap<Position, DataSet>();
	private Map<DataSet, Set<Position>> dataSetsWithPositions = new HashMap<DataSet, Set<Position>>();

	private Set<Alias> aliases = new HashSet<Alias>();

	private boolean isLocation = false;
	private int isLocationCount = 0;
	private int isNoLocationCount = 0;

	private boolean isActor = false;

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
		this.startTimes.add(startTime);
		if (!this.startTimesWithDataSets.containsKey(startTime))
			startTimesWithDataSets.put(startTime, new HashSet<DataSet>());
		this.startTimesWithDataSets.get(startTime).add(dataSet);
		this.dataSetsWithStartTimes.put(dataSet, startTime);
	}

	public void addEndTime(DateWithGranularity endTime, DataSet dataSet) {
		this.endTimes.add(endTime);
		if (!this.endTimesWithDataSets.containsKey(endTime))
			endTimesWithDataSets.put(endTime, new HashSet<DataSet>());
		this.endTimesWithDataSets.get(endTime).add(dataSet);
		this.dataSetsWithEndTimes.put(dataSet, endTime);
	}

	public Set<DateWithGranularity> getStartTimes() {
		return startTimes;
	}

	public Map<DateWithGranularity, Set<DataSet>> getStartTimesWithDataSets() {
		return startTimesWithDataSets;
	}

	public Set<DateWithGranularity> getEndTimes() {
		return endTimes;
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

	public Map<DataSet, DateWithGranularity> getDataSetsWithStartTimes() {
		return dataSetsWithStartTimes;
	}

	public Map<DataSet, DateWithGranularity> getDataSetsWithEndTimes() {
		return dataSetsWithEndTimes;
	}

	public void addPosition(Position position, DataSet dataSet) {
		this.positions.add(position);

		this.positionsWithDataSets.put(position, dataSet);

		if (!this.dataSetsWithPositions.containsKey(dataSet))
			this.dataSetsWithPositions.put(dataSet, new HashSet<Position>());

		this.dataSetsWithPositions.get(dataSet).add(position);
	}

	public Set<Position> getPositions() {
		return positions;
	}

	public Set<Position> getPositionsOfDataSet(DataSet dataSet) {
		return dataSetsWithPositions.get(dataSet);
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

}
