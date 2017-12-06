package de.l3s.eventkg.nlp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.meta.Language;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

public class OpenNLPutils {
	private SentenceModel model;
	private SentenceDetectorME sentenceDetector;

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
