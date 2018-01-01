package de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.Page;
import org.mediawiki.importer.Revision;
import org.mediawiki.importer.Siteinfo;
import org.mediawiki.importer.Wikiinfo;

import de.l3s.eventkg.meta.Language;

public class EventKGExtractor implements DumpWriter {

	String pageTitle = "";
	int _targetPageId;
	int _pageId;
	boolean debug = false;
	String _page = "";
	boolean empty = true;
	String path;
	boolean pageIsMainArticle = false;
	// Set<Integer> _pageIdsWithEvents;
	BufferedWriter fileEvents;
	BufferedWriter fileFirstSentences;
	BufferedWriter fileLinkSets;
	BufferedWriter fileLinkCounts;

	private Language language;
	private Map<String, String> redirects;

	protected static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.S");

	public void close() throws IOException {
	}

	public EventKGExtractor(String pageId, Language language, BufferedWriter fileEvents,
			BufferedWriter fileFirstSentences, BufferedWriter fileLinkSets, BufferedWriter fileLinkCounts,
			Map<String, String> redirects) {
		this._targetPageId = Integer.parseInt(pageId);
		this.language = language;
		this.fileEvents = fileEvents;
		this.fileFirstSentences = fileFirstSentences;
		this.fileLinkSets = fileLinkSets;
		this.fileLinkCounts = fileLinkCounts;
		this.redirects = redirects;
		// this._pageIdsWithEvents = pageIdsWithEvents;
	}

	public void writeStartWiki(Wikiinfo info) throws IOException {
	}

	public void writeEndWiki() throws IOException {
	}

	public void writeSiteinfo(Siteinfo info) throws IOException {
	}

	public void writeStartPage(Page page) throws IOException {
		this.empty = true;
		this._pageId = page.Id;
		this.pageTitle = page.Title.Text;
		if (page.Ns == 0 && !page.isRedirect) {
			this.pageIsMainArticle = true;
		}
	}

	public void writeEndPage() throws IOException {
		if (this._pageId == this._targetPageId || this._targetPageId == -1) {
			this.fileEvents.flush();
			this.fileFirstSentences.flush();
			this.fileLinkCounts.flush();
			this.fileLinkSets.flush();
		}
		this.pageIsMainArticle = false;
	}

	public void writeRevision(Revision revision) throws IOException {

		if (this.pageIsMainArticle) {

			System.out.println("Wiki Page: " + String.valueOf(this._pageId) + ": " + this.pageTitle);

			TextExtractorNew extractor = new TextExtractorNew(revision.Text, this._pageId, true, language,
					this.pageTitle, redirects);
			try {
				extractor.extractLinks();
			} catch (Exception e) {
				System.err.println("Error with " + this._pageId + ": " + this.pageTitle);
				e.printStackTrace();
			}

			String prefix = this._pageId + "\t" + this.pageTitle + "\t";

			Output output = extractor.getOutput();
			if (output.getFirstSentence() != null && !output.getFirstSentence().isEmpty()) {
				this.fileFirstSentences.append(prefix + output.getFirstSentence() + "\n");
			}
			if (!output.getEnrichedCounts().isEmpty()) {
				this.fileLinkCounts.append(prefix + output.getLinkCountsEnrichedInOneLine() + "\n");
			}
			if (!output.getLinksInCommonSentencesEnriched().isEmpty()) {
				this.fileLinkSets.append(prefix + output.getLinkSetsInLines() + "\n");
			}

			// extract events

			EventExtractorFromYearPages eventsExtractor = new EventExtractorFromYearPages(revision.Text, this._pageId,
					this.pageTitle, language, redirects);
			if (eventsExtractor.isYearOrDayPage()) {
				System.out.println("Date page: " + String.valueOf(this._pageId) + ": " + this.pageTitle);
				eventsExtractor.extractEvents();
				if (!eventsExtractor.getEventsOutput().isEmpty()) {
					this.fileEvents.append(eventsExtractor.getEventsOutput() + "\n");
				}
			}
		}
	}
}
