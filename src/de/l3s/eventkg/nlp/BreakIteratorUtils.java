package de.l3s.eventkg.nlp;

import java.io.FileNotFoundException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import jep.JepException;
import opennlp.tools.util.Span;

public class BreakIteratorUtils implements NLPUtils {

	private BreakIterator boundary;

	public static void main(String[] args) throws JepException, FileNotFoundException {

		System.out.println(Locale.forLanguageTag("PT"));

		for (Locale loc : BreakIterator.getAvailableLocales()) {
			System.out.println(loc);
		}

		System.out.println(
				"res = ''\nfor sentence in sentencesDe :\n\tfor token in sentence:\n\t\tline += token.word + '\t'\n\t\tline += token.space_after\n\t\tres += line + '\\n'");

		Set<String> sentences = new HashSet<String>();
		// sentences.add("Dies ist ein Satz. Und hier ist noch ein Satz.");
		// sentences.add("Morgen ist's Wochenende. Ich weiß, das ist gut.");
		// sentences.add("Pius II. ging nach Hause. Das war in Rom. Später am
		// 13. März war Regen.");
		sentences.add(
				"Die  Skulpturenmeile in Hannover ist ein zwischen 1986 und 2000 geschaffener Skulpturenweg entlang der Straßen Brühlstraße und Leibnizufer in Hannover. Er besteht aus übergroßen Skulpturen und Plastiken im öffentlichen Straßenraum. Die Kunstwerke sind auf einer Länge von etwa 1.200 Meter zwischen dem Königsworther Platz und dem Niedersächsischen Landtag aufgestellt. Die sehr unterschiedlichen Arbeiten befinden sich überwiegend auf der grünen Mittelinsel des sechsspurigen, stark befahrenen Straßenzuges.");

		BreakIteratorUtils nlpUtils = new BreakIteratorUtils(Language.DE);

		for (String sentence : sentences) {
			for (Span span : nlpUtils.sentenceSplitterPositions(sentence)) {
				System.out.println(span.getCoveredText(sentence));
				System.out.println(" " + span.getStart() + ", " + span.getEnd());
			}
		}

	}

	public BreakIteratorUtils(Language language) {
		this.boundary = BreakIterator.getSentenceInstance(language.getLocale());
	}

	@Override
	public Span[] sentenceSplitterPositions(String text) {

		boundary.setText(text);

		List<Span> spans = new ArrayList<Span>();

		int start = boundary.first();
		for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {

			String coveredTextWithSpace = text.substring(start, end);

			int numberOfSpacesAtTheEnd = 0;
			String coveredText = coveredTextWithSpace;
			while (coveredText.endsWith(" ")) {
				numberOfSpacesAtTheEnd += 1;
				coveredText = coveredText.substring(0, coveredText.length() - 1);
			}

			int correctedEnd = end - numberOfSpacesAtTheEnd;
			Span span = new Span(start, correctedEnd);
			spans.add(span);
		}

		Span[] spanArray = new Span[spans.size()];
		for (int i = 0; i < spanArray.length; i++)
			spanArray[i] = spans.get(i);
		return spanArray;
	}

}
