package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import de.l3s.eventkg.source.wikipedia.mwdumper.model.Link;
import de.l3s.eventkg.source.wikipedia.mwdumper.model.Sentence;

public class Output {

	private String firstSentence;

	private Map<String, Integer> counts = new HashMap<String, Integer>();

	private Map<String, Integer> enrichedCounts = new HashMap<String, Integer>();

	private List<Set<Link>> linksInCommonSentences = new ArrayList<Set<Link>>();

	private List<Set<Link>> linksInCommonSentencesEnriched = new ArrayList<Set<Link>>();

	private List<Sentence> sentences;

	public String getFirstSentence() {
		return firstSentence;
	}

	public void setFirstSentence(String firstSentence) {
		this.firstSentence = firstSentence;
	}

	public Map<String, Integer> getCounts() {
		return counts;
	}

	public void setCounts(Map<String, Integer> counts) {
		this.counts = counts;
	}

	public Map<String, Integer> getEnrichedCounts() {
		return enrichedCounts;
	}

	public void setEnrichedCounts(Map<String, Integer> enrichedCounts) {
		this.enrichedCounts = enrichedCounts;
	}

	public List<Set<Link>> getLinksInCommonSentences() {
		return linksInCommonSentences;
	}

	public void setLinksInCommonSentences(List<Set<Link>> linksInCommonSentences) {
		this.linksInCommonSentences = linksInCommonSentences;
	}

	public List<Set<Link>> getLinksInCommonSentencesEnriched() {
		return linksInCommonSentencesEnriched;
	}

	public void setLinksInCommonSentencesEnriched(List<Set<Link>> linksInCommonSentencesEnriched) {
		this.linksInCommonSentencesEnriched = linksInCommonSentencesEnriched;
	}

	public void addLinksInSentenceSet(Set<Link> linksInSentence) {
		this.linksInCommonSentences.add(linksInSentence);
	}

	public void addLinksInSentenceSetEnriched(Set<Link> linksInSentenceEnriched) {
		this.linksInCommonSentencesEnriched.add(linksInSentenceEnriched);
	}

	public void addLink(String entityName) {
		if (!this.counts.containsKey(entityName)) {
			this.counts.put(entityName, 1);
		} else {
			this.counts.put(entityName, this.counts.get(entityName) + 1);
		}

	}

	public void addLinkEnriched(String entityName) {
		if (!this.enrichedCounts.containsKey(entityName)) {
			this.enrichedCounts.put(entityName, 1);
		} else {
			this.enrichedCounts.put(entityName, this.enrichedCounts.get(entityName) + 1);
		}

	}

	public String getLinkCountsEnrichedInOneLine() {
		Set<String> counts = new HashSet<String>();

		for (String link : this.enrichedCounts.keySet()) {
			counts.add(removeSpacesAndTabs(link) + " " + this.enrichedCounts.get(link));
		}

		return StringUtils.join(counts, "\t");
	}

	public String getLinkSetsInLines() {
		Set<String> sets = new HashSet<String>();

		for (Set<Link> linksSet : this.linksInCommonSentencesEnriched) {
			Set<String> links = new HashSet<String>();
			for (Link link : linksSet) {
				links.add(removeSpacesAndTabs(link.getName()));
			}
			sets.add(StringUtils.join(links, " "));
		}

		return StringUtils.join(sets, "\t");
	}

	private String removeSpacesAndTabs(String term) {
		return term.replaceAll("\t", "").replaceAll(" ", "_");
	}

	public List<Sentence> getSentences() {
		return sentences;
	}

	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}

}
