package de.l3s.eventkg.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class DataSets {

	private Map<Language, Map<Source, DataSet>> dataSets = new HashMap<Language, Map<Source, DataSet>>();

	private Map<Source, DataSet> dataSetsWithoutLanguage = new HashMap<Source, DataSet>();

	private Set<DataSet> allDataSets = new HashSet<DataSet>();

	private Map<String, DataSet> dataSetsById = new HashMap<String, DataSet>();

	private static DataSets instance;

	public static DataSets getInstance() {
		if (instance == null) {
			instance = new DataSets();
		}
		return instance;
	}

	private DataSets() {
	}

	public void addDataSet(Language language, Source source, String url) {
		if (!this.dataSets.containsKey(language))
			this.dataSets.put(language, new HashMap<Source, DataSet>());
		DataSet dataSet = new DataSet(source,
				"<graphs/" + source.toString().toLowerCase() + "_" + language.getLanguageLowerCase() + ">",
				"<" + url + ">");
		allDataSets.add(dataSet);
		this.dataSets.get(language).put(source, dataSet);
		this.dataSetsById.put(dataSet.getId(), dataSet);
	}

	public void addDataSetWithoutLanguage(Source source, String url) {
		DataSet dataSet = new DataSet(source, "<graphs/" + source.toString().toLowerCase() + ">", "<" + url + ">");
		allDataSets.add(dataSet);
		this.dataSetsWithoutLanguage.put(source, dataSet);
		this.dataSetsById.put(dataSet.getId(), dataSet);
	}

	public DataSet getDataSet(Language language, Source source) {
		return this.dataSets.get(language).get(source);
	}

	public DataSet getDataSetWithoutLanguage(Source source) {
		return this.dataSetsWithoutLanguage.get(source);
	}

	public Set<DataSet> getAllDataSets() {
		return this.allDataSets;
	}

	public DataSet getDataSetById(String dataSetId) {
		return this.dataSetsById.get(dataSetId);
	}

}
