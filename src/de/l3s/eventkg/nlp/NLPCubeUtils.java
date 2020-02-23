package de.l3s.eventkg.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;
import opennlp.tools.util.Span;

public class NLPCubeUtils implements NLPUtils {

	private Interpreter interp;

	// // returns sentence annotations - but just the strings
	// public String[] sentenceSplitter(String text) {
	// String[] sentences = sentenceDetector.sentDetect(text);
	// return sentences;
	// }

	// -Djava.library.path=/home/simon/.local/lib/python3.5/site-packages/jep
	// -Djava.library.path=/usr/local/lib/python2.7/dist-packages/jep

	// What was done?
	// Jep in maven
	// pip install jep

	public static void main(String[] args) throws JepException {

		System.out.println(
				"res = ''\nfor sentence in sentencesDe :\n\tfor token in sentence:\n\t\tline += token.word + '\t'\n\t\tline += token.space_after\n\t\tres += line + '\\n'");

		Set<String> sentences = new HashSet<String>();
		// sentences.add("Dies ist ein Satz. Und hier ist noch ein Satz.");
//		sentences.add("Morgen ist's Wochenende. Ich weiß, das ist gut.");
		// sentences.add("Pius II. ging nach Hause. Das war in Rom.");

		sentences.add("Die  Skulpturenmeile in Hannover ist ein zwischen 1986 und 2000 geschaffener Skulpturenweg entlang der Straßen Brühlstraße und Leibnizufer in Hannover. Er besteht aus übergroßen Skulpturen und Plastiken im öffentlichen Straßenraum. Die Kunstwerke sind auf einer Länge von etwa 1.200 Meter zwischen dem Königsworther Platz und dem Niedersächsischen Landtag aufgestellt. Die sehr unterschiedlichen Arbeiten befinden sich überwiegend auf der grünen Mittelinsel des sechsspurigen, stark befahrenen Straßenzuges.");
		
		NLPCubeUtils nlpUtils = new NLPCubeUtils(Language.DE);

		for (String sentence : sentences) {
			for (Span span : nlpUtils.sentenceSplitterPositions(sentence)) {
				System.out.println(span.getCoveredText(sentence));
				System.out.println(" " + span.getStart() + ", " + span.getEnd());
			}
		}

	}

	public NLPCubeUtils(Language language) throws JepException {

		this.interp = new SharedInterpreter();
		interp.eval("import sys");
		interp.eval("if not hasattr(sys, 'argv'):\n	sys.argv  = ['']");
		interp.eval("import sys");

		interp.eval("from cube.api import Cube");
		interp.eval("cube=Cube(verbose=True)");
		interp.eval("cube.load('" + language.getLanguageLowerCase() + "')");
	}

	// returns sentence annotations - but just the spans
	public Span[] sentenceSplitterPositions(String text) {
		
		System.out.println("Split: "+text);

		try {
			text=text.replace("'", "\\'");
			interp.eval("text='" + text + "'");
			interp.eval("sentences=cube(text)");

			Object result1 = interp.getValue("sentences");

			String res = result1.toString();

			List<Span> spans = new ArrayList<Span>();

			int startPosition = 0;
			int endPosition = 0;
			for (String sentenceRes : res.split("\\], \\[")) {
				// sentenceRes = sentenceRes.substring(2,
				// sentenceRes.length() - 2);
				String[] parts = sentenceRes.split("\t");
				String space = null;
				for (int i = 1; i < parts.length; i += 9) {
					space = parts[i + 8];
					if (space.contains(","))
						space = space.substring(0, space.indexOf(","));

					endPosition += parts[i].length();

					if (!space.contains("SpaceAfter=No"))
						endPosition += 1;
				}

				if (!space.contains("SpaceAfter=No"))
					endPosition -= 1;

				Span span = new Span(startPosition, endPosition);
				spans.add(span);

				if (!space.contains("SpaceAfter=No"))
					endPosition += 1;

				startPosition = endPosition;
			}

			Span[] spanArray = new Span[spans.size()];
			for (int i = 0; i < spanArray.length; i++)
				spanArray[i] = spans.get(i);
			return spanArray;

		} catch (JepException e) {
			e.printStackTrace();
		}

		return null;
	}

}
