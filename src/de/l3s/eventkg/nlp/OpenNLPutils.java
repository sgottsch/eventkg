package de.l3s.eventkg.nlp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import jep.JepException;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

public class OpenNLPutils implements NLPUtils {

	private SentenceModel model;
	private SentenceDetectorME sentenceDetector;

	public static void main(String[] args) throws JepException, FileNotFoundException {

		System.out.println(
				"res = ''\nfor sentence in sentencesDe :\n\tfor token in sentence:\n\t\tline += token.word + '\t'\n\t\tline += token.space_after\n\t\tres += line + '\\n'");

		Set<String> sentences = new HashSet<String>();
		// sentences.add("Dies ist ein Satz. Und hier ist noch ein Satz.");
		// sentences.add("Morgen ist's Wochenende. Ich weiß, das ist gut.");
		// sentences.add("Pius II. ging nach Hause. Das war in Rom.");
		sentences.add(
				"Die  Skulpturenmeile in Hannover ist ein zwischen 1986 und 2000 geschaffener Skulpturenweg entlang der Straßen Brühlstraße und Leibnizufer in Hannover. Er besteht aus übergroßen Skulpturen und Plastiken im öffentlichen Straßenraum. Die Kunstwerke sind auf einer Länge von etwa 1.200 Meter zwischen dem Königsworther Platz und dem Niedersächsischen Landtag aufgestellt. Die sehr unterschiedlichen Arbeiten befinden sich überwiegend auf der grünen Mittelinsel des sechsspurigen, stark befahrenen Straßenzuges.");

		OpenNLPutils nlpUtils = new OpenNLPutils(Language.DE);

		for (String sentence : sentences) {
			for (Span span : nlpUtils.sentenceSplitterPositions(sentence)) {
				System.out.println(span.getCoveredText(sentence));
				System.out.println(" " + span.getStart() + ", " + span.getEnd());
			}
		}

	}

	public OpenNLPutils(Language lang) throws FileNotFoundException {

		InputStream modelIn = null;

		modelIn = OpenNLPutils.class
				.getResourceAsStream("/resource/lang/" + lang.getLanguage().toLowerCase() + "-sent.bin");
		if (modelIn == null)
			System.err.println("OpenNLPutils:  Sentence splitting in lang not supported: " + lang.getLanguage());

		try {
			model = new SentenceModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}

		this.sentenceDetector = new SentenceDetectorME(this.model);
	}

	// returns sentence annotations - but just the strings
	public String[] sentenceSplitter(String text) {
		String[] sentences = sentenceDetector.sentDetect(text);
		return sentences;
	}

	/**
	 * returns sentence annotations - but just the strings very small sentences
	 * are joined to the previous one
	 * 
	 * @param text
	 *            Text to split into sentences
	 * @param minimumLength
	 *            minimum length of a sentence that can be returned as a single
	 *            sentence
	 * @return sentences found in the text
	 */
	public String[] sentenceSplitterAndJoinSmallOnes(String text, int minimumLength) {
		String[] originalSentences = sentenceDetector.sentDetect(text);

		List<String> sentencesList = new ArrayList<String>();

		String previous = null;
		for (String s : originalSentences) {
			if (s.length() < minimumLength) {
				if (previous == null || previous.isEmpty())
					previous = s;
				else
					previous = previous + " " + s;
			} else {
				if (previous != null)
					sentencesList.add(previous);
				previous = s;
			}
		}
		sentencesList.add(previous);

		return sentencesList.toArray(new String[sentencesList.size()]);
	}

	// returns sentence annotations - but just the spans
	public Span[] sentenceSplitterPositions(String text) {
		Span[] sentences = sentenceDetector.sentPosDetect(text);
		return sentences;
	}

}
