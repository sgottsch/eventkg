package de.l3s.eventkg.nlp;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import jep.JepException;
import opennlp.tools.util.Span;

public class OpenNLPOrBreakIteratorNLPUtils implements NLPUtils {

	NLPUtils nlpUtils;

	public static void main(String[] args) throws JepException, FileNotFoundException {

		System.out.println(
				"res = ''\nfor sentence in sentencesDe :\n\tfor token in sentence:\n\t\tline += token.word + '\t'\n\t\tline += token.space_after\n\t\tres += line + '\\n'");

		Set<String> sentences = new HashSet<String>();
		// sentences.add("Dies ist ein Satz. Und hier ist noch ein Satz.");
		// sentences.add("Morgen ist's Wochenende. Ich weiß, das ist gut.");
		// sentences.add("Pius II. ging nach Hause. Das war in Rom.");
		sentences.add(
				"Die  Skulpturenmeile in Hannover ist ein zwischen 1986 und 2000 geschaffener Skulpturenweg entlang der Straßen Brühlstraße und Leibnizufer in Hannover. Er besteht aus übergroßen Skulpturen und Plastiken im öffentlichen Straßenraum. Die Kunstwerke sind auf einer Länge von etwa 1.200 Meter zwischen dem Königsworther Platz und dem Niedersächsischen Landtag aufgestellt. Die sehr unterschiedlichen Arbeiten befinden sich überwiegend auf der grünen Mittelinsel des sechsspurigen, stark befahrenen Straßenzuges.");

		OpenNLPOrBreakIteratorNLPUtils nlpUtils = new OpenNLPOrBreakIteratorNLPUtils(Language.DE);

		for (String sentence : sentences) {
			for (Span span : nlpUtils.sentenceSplitterPositions(sentence)) {
				System.out.println(span.getCoveredText(sentence));
				System.out.println(" " + span.getStart() + ", " + span.getEnd());
			}
		}

	}

	public OpenNLPOrBreakIteratorNLPUtils(Language language) throws FileNotFoundException {

		InputStream modelIn = OpenNLPutils.class
				.getResourceAsStream("/resource/lang/" + language.getLanguage().toLowerCase() + "-sent.bin");
		if (modelIn != null) {
			// System.out.println("Init OpenNLP for " + language.getLanguage() +
			// ".");
			this.nlpUtils = new OpenNLPutils(language);
		} else {
			// System.out.println("Init BreakIterator for " +
			// language.getLanguage() + ".");
			this.nlpUtils = new BreakIteratorUtils(language);
		}

	}

	public Span[] sentenceSplitterPositions(String text) {
		return this.nlpUtils.sentenceSplitterPositions(text);
	}

}
