package de.l3s.eventkg.wikipedia.mwdumper.articleprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.wikipedia.mwdumper.model.Paragraph;
import de.l3s.eventkg.wikipedia.mwdumper.model.Reference;

public class ReferenceAndTemplateRemover {

	private Language language;

	private static ReferenceAndTemplateRemover singleton;
	private List<Pattern> referencePatterns;
	private Pattern furtherPagesPattern;
	private Pattern templateRemovePattern;
	private Pattern linksFindPattern;
	// private Pattern simpleDatePattern;
	private Pattern refPattern;
	private Pattern commentPattern;
	private Pattern wikiTableRemovePattern;
	private Pattern divRemovePattern;
	private Pattern subRemovePattern;
	private Pattern supRemovePattern;
	private Pattern galleryRemovePattern;
	private Pattern commaSeparatedNumbersPattern;
	private Pattern smallRemovePattern;
	private Pattern blockQuoteRemovePattern;
	private Pattern imageMapRemovePattern;
	// private Pattern imageRemovePattern;

	private static final String REF_BEGIN_STRING = "#<R#";
	private static final String REF_END_STRING = "#>R#";

	private ReferenceAndTemplateRemover(Language language) {
		this.language = language;
	}

	public static synchronized ReferenceAndTemplateRemover getInstance(Language language) {
		if (singleton == null) {
			singleton = new ReferenceAndTemplateRemover(language);
			singleton.init();
		}
		return singleton;
	}

	public void init() {
		this.referencePatterns = new ArrayList<Pattern>();
		String r1 = "<[Rr]ef[^>]*?/>";
		String r2 = "<[Rr]ef[^>]*?>.*?</[Rr]ef>";
		String r3 = "\\[https?://[^\\]]*\\]";
		String r8 = "<!--.*?-->";

		this.furtherPagesPattern = Pattern.compile(
				"\\{\\{(main|Main|See also|see also|Further|further|further2|Further2|details|Details|outline|Outline|Details3|details3)\\|[^\\{\\}]*\\}\\}");
		// this.templateRemovePattern =
		// Pattern.compile("\\{\\{[^\\{\\}]*\\}\\}", 32);

		// negative look-ahead to not find "{{" or "}}" within the "{{..}}"
		this.templateRemovePattern = Pattern.compile("\\{\\{((?!\\{\\{|\\}\\}).)*\\}\\}",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.wikiTableRemovePattern = Pattern.compile("\\{\\|[^\\{\\}]*\\|\\}",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		// for DIVs take care, as there are some difficult nested cases.
		// Source:
		// http://stackoverflow.com/questions/611883/regex-how-to-match-everything-except-a-particular-pattern
		this.divRemovePattern = Pattern.compile("<div[^>]*>((?!<div).)*?</div>",
				Pattern.CASE_INSENSITIVE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		// this.imageRemovePattern = Pattern.compile(
		// "\\[\\[" + WikiWords.getInstance().getImageLabel(language) +
		// ":.*?\\]\\]",
		// Pattern.CASE_INSENSITIVE | Pattern.CASE_INSENSITIVE |
		// Pattern.DOTALL);
		this.smallRemovePattern = Pattern.compile("<small[^>]*>(.*?)</small>",
				Pattern.CASE_INSENSITIVE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.blockQuoteRemovePattern = Pattern.compile("<blockquote[^>]*>(.*?)</blockquote>",
				Pattern.CASE_INSENSITIVE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.galleryRemovePattern = Pattern.compile("<gallery[^>]*>.*?</gallery>",
				Pattern.CASE_INSENSITIVE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.subRemovePattern = Pattern.compile("<sub[^>]*>(.*?)</sub>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.supRemovePattern = Pattern.compile("<sup[^>]*>(.*?)</sup>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		this.imageMapRemovePattern = Pattern.compile("<imagemap[^>]*>(.*?)</imagemap>",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		// this.squaredBracketLinksPattern = Pattern.compile("\\[http.*\\]",
		// 32);
		this.commaSeparatedNumbersPattern = Pattern.compile("([0-9]*[1-9]+)\\,([0-9]+)");

		this.linksFindPattern = Pattern.compile("\\[\\[[^\\[\\]]*\\]\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Pattern p1 = Pattern.compile(r1, 32);
		this.referencePatterns.add(p1);
		Pattern p2 = Pattern.compile(r2, 32);
		this.referencePatterns.add(p2);
		Pattern p3 = Pattern.compile(r3, 32);
		this.referencePatterns.add(p3);

		this.commentPattern = Pattern.compile(r8, 32);
		this.referencePatterns.add(this.commentPattern);
		this.refPattern = Pattern.compile(p1 + "|" + p2 + "|" + p3, 32);
		// List<String> monthNames =
		// WikiWords.getInstance().getMonthNames(language);
		// String regexMonth = "(" + StringUtils.join(monthNames, "|") + ")";
		// String yearRegex = "([1-2][0-9]{3})";
		// String dayRegex = "([1-9]|[1-2][0-9]|3[0-1])";
		// String monthDayYear = String.valueOf(regexMonth) + " " + dayRegex +
		// ", " + yearRegex;
		// this.simpleDatePattern = Pattern.compile(monthDayYear);
	}

	public String removeReferences(String text) {
		return this.removeReferences(null, text);
	}

	public String removeReferences(Paragraph paragraph, String text) {
		ArrayList<Reference> references = new ArrayList<Reference>();
		int offset = 0;
		Matcher m = this.refPattern.matcher(text);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String groupText = m.group();
			m.appendReplacement(sb, "");
			if (!groupText.contains("http") && !groupText.contains("url")) {
				offset -= m.group().length();
				continue;
			}
			try {
				if (!groupText.contains("url")) {
					Reference reference = new Reference();
					reference.setStartPosition(m.start() + offset - 1);
					reference.setOriginalStartPosition(reference.getStartPosition());
					reference.setType("brackets");
					references.add((Reference) reference);
					groupText = groupText.substring(groupText.indexOf(">") + 1).trim();
					String urlBracketPart = groupText.substring(groupText.indexOf("[http") + 1);
					int bracketPartPos = urlBracketPart.indexOf("]");
					if (bracketPartPos == -1) {
						offset -= m.group().length();
						continue;
					}
					urlBracketPart = urlBracketPart.substring(0, bracketPartPos);
					int restPartPos = groupText.indexOf("[http") + 1 + urlBracketPart.length() + 1;
					if (urlBracketPart.contains(" ")) {
						int pos = urlBracketPart.indexOf(" ");
						reference.setUrl(urlBracketPart.substring(0, pos));
						reference.setTitle(urlBracketPart.substring(pos + 1));
					} else {
						reference.setUrl(urlBracketPart);
					}
					String restPart = groupText.substring(restPartPos);
					if (restPart.toLowerCase().contains("new york times")) {
						reference.setSource("The New York Times");
					}
					reference.setWholeInformationString(restPart.replaceAll("\t", "   "));
					if (restPart.contains("Retrieved ")) {
						restPart = restPart.substring(0, restPart.indexOf("Retrieved "));
					}
					// Matcher mDate = this.simpleDatePattern.matcher(restPart);
					// while (mDate.find()) {
					// reference.setPublicationDate("GR:" + mDate.group());
					// }
				} else {
					if (!groupText.contains(REF_BEGIN_STRING) && !groupText.contains("{{")) {
						offset -= m.group().length();
						continue;
					}
					groupText = groupText.substring(groupText.indexOf(REF_BEGIN_STRING) + 4,
							groupText.indexOf(REF_END_STRING));
					Reference reference = new Reference();
					reference.setStartPosition(m.start() + offset - 1);
					reference.setOriginalStartPosition(reference.getStartPosition());
					references.add((Reference) reference);
					reference.setWholeInformationString(groupText.replaceAll("\t", "   "));
					Pattern pLink = ReferenceAndTemplateRemover.getInstance(language).getLinksFindPattern();
					Matcher mLink = pLink.matcher(groupText);
					StringBuffer sb2 = new StringBuffer();
					while (mLink.find()) {
						String linkName;
						String anchorText = linkName = mLink.group().substring(2, mLink.group().length() - 2);
						if (linkName.contains("|")) {
							anchorText = linkName.substring(linkName.indexOf("|") + 1, linkName.length());
						}
						anchorText = Matcher.quoteReplacement(anchorText);
						mLink.appendReplacement(sb2, anchorText);
					}
					mLink.appendTail(sb2);
					groupText = sb2.toString();
					String[] parts = groupText.split("\\|");
					String type = parts[0].toLowerCase().trim();
					reference.setType(type);
					int i = -1;
					String[] arrstring = parts;
					int n = arrstring.length;
					int n2 = 0;
					while (n2 < n) {
						String part = arrstring[n2];
						if (++i != 0 && !part.trim().isEmpty() && part.contains("=")) {
							String key = part.substring(0, part.indexOf("=")).trim();
							String value = part.substring(part.indexOf("=") + 1).trim();
							if (key.equals("title")) {
								reference.setTitle(value);
							} else if (key.equals("url")) {
								reference.setUrl(value);
							} else if (key.equals("date")) {
								reference.setPublicationDate(value);
							} else if (key.equals("title")) {
								reference.setTitle(value);
							} else if (key.equals("work")) {
								reference.setSource(value);
							} else if (key.equals("publisher")) {
								reference.setSource(value);
							}
						}
						++n2;
					}
				}
				offset -= m.group().length();
				continue;
			} catch (StringIndexOutOfBoundsException reference) {
				// empty catch block
			}
		}
		m.appendTail(sb);
		text = sb.toString();
		if (paragraph != null) {
			paragraph.setReferences(references);
		}
		return text;
	}

	public String removeWikiTablesAndDivs(String text) {
		StringBuffer sb;
		Matcher m;
		boolean changed = true;
		while (changed) {
			changed = false;
			m = this.wikiTableRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				String pref = "";
				if (m.start() >= 3 && !(pref = text.substring(m.start() - 3, m.start())).endsWith("\n")
						&& (pref = pref.trim()).length() == 3) {
					pref = pref.substring(0, 2);
				}
				if (pref.equals("=="))
					continue;
				m.appendReplacement(sb, "");
				changed = true;
			}
			m.appendTail(sb);
			text = sb.toString();
		}
		changed = true;
		while (changed) {
			changed = false;
			m = this.divRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				changed = true;
				m.appendReplacement(sb, "");
			}
			m.appendTail(sb);
			text = sb.toString();
		}
		changed = true;

		while (changed) {
			changed = false;
			m = this.galleryRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(sb, "");
				changed = true;
			}
			m.appendTail(sb);
			text = sb.toString();
		}

		changed = true;

		while (changed) {
			changed = false;
			m = this.blockQuoteRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				String newText = Matcher.quoteReplacement(m.group(1));
				m.appendReplacement(sb, newText);
			}
			m.appendTail(sb);
			text = sb.toString();
		}

		changed = true;

		while (changed) {
			changed = false;
			m = this.smallRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				String newText = Matcher.quoteReplacement(m.group(1));
				m.appendReplacement(sb, newText);
			}
			m.appendTail(sb);
			text = sb.toString();
		}
		// found the case:
		// '''Marcus Aemilius Lepidus'''
		// ({{lang-la|<small>M·AEMILIVS·M·F·Q·N·LEPIDVS}}),</small>
		// => parts in brackets is removed first, s.t. tags are not removed
		text = text.replaceAll("<small[^>]*>", "");
		text = text.replaceAll("</small>", "");

		changed = true;

		while (changed) {
			changed = false;
			m = this.subRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				String newText = Matcher.quoteReplacement(m.group(1));
				m.appendReplacement(sb, newText);
			}
			m.appendTail(sb);
			text = sb.toString();
		}
		text = text.replaceAll("<sub[^>]*>", "");
		text = text.replaceAll("</sub>", "");

		changed = true;

		while (changed) {
			changed = false;
			m = this.supRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				String newText = Matcher.quoteReplacement(m.group(1));
				m.appendReplacement(sb, newText);
				changed = true;
			}
			m.appendTail(sb);
			text = sb.toString();
		}
		text = text.replaceAll("<sup[^>]*>", "");
		text = text.replaceAll("</sup>", "");

		changed = true;

		while (changed) {
			changed = false;
			m = this.imageMapRemovePattern.matcher(text);
			sb = new StringBuffer();
			while (m.find()) {
				m.appendReplacement(sb, "");
				changed = true;
			}
			m.appendTail(sb);
			text = sb.toString();
		}

		// changed = true;
		//
		// while (changed) {
		// changed = false;
		// m = this.imageRemovePattern.matcher(text);
		// sb = new StringBuffer();
		// while (m.find()) {
		// m.appendReplacement(sb, "");
		// changed = true;
		// }
		// m.appendTail(sb);
		// text = sb.toString();
		// }

		// int pos = text.indexOf("[[Image:");
		// System.out.println(pos);
		// int cnt = 0;
		// while (pos != -1) {
		// System.out.println(cnt+", "+pos);
		// String textAfter = text.substring(pos, text.indexOf("]]"));
		// cnt += StringUtils.countMatches(textAfter, "[[");
		// text = text.substring(0, pos) + text.substring(text.indexOf("]]"));
		//
		// pos = text.indexOf("[[Image:");
		//
		// cnt -= 1;
		// }

		text = text.replaceAll("<br>", " ");
		text = text.replaceAll("<br/>", " ");
		text = text.replaceAll("<br />", " ");
		text = text.replaceAll("<tt>", " ");
		text = text.replaceAll("</tt>", " ");

		return text;
	}

	public String removeTemplates(String text) {

		Matcher mComment = this.commentPattern.matcher(text);
		StringBuffer sbComment = new StringBuffer();
		while (mComment.find()) {
			mComment.appendReplacement(sbComment, "");
		}
		mComment.appendTail(sbComment);
		text = sbComment.toString();
		Matcher mRef2 = this.refPattern.matcher(text);
		StringBuffer sbRef2 = new StringBuffer();
		while (mRef2.find()) {
			String withoutLineBreaks = Matcher.quoteReplacement(mRef2.group().replaceAll("\n", " "));
			mRef2.appendReplacement(sbRef2, withoutLineBreaks);
		}
		mRef2.appendTail(sbRef2);
		text = sbRef2.toString();
		boolean changed = true;
		while (changed) {
			changed = false;
			Matcher m = this.templateRemovePattern.matcher(text);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String succ;
				String pref = "";
				String groupText = m.group();
				if (m.start() >= 3 && !(pref = text.substring(m.start() - 3, m.start())).endsWith("\n")
						&& (pref = pref.trim()).length() == 3) {
					pref = pref.substring(0, 2);
				}
				if (text.length() >= m.end() + 6
						&& ((succ = text.substring(m.end(), m.end() + 6)).toLowerCase().equals("</ref>")
								|| succ.toLowerCase().equals("</Ref>"))) {
					String refString = REF_BEGIN_STRING + groupText.substring(2, groupText.length() - 2)
							+ REF_END_STRING;
					refString = Matcher.quoteReplacement(refString);
					m.appendReplacement(sb, refString);
					changed = true;
					continue;
				}
				if (pref.equals("=="))
					continue;
				m.appendReplacement(sb, "");
				changed = true;
			}
			m.appendTail(sb);
			text = sb.toString();
			// System.out.println("\n\n\n" + text + "\n\n\n");
		}
		text = this.removeWikiTablesAndDivs(text);
		return text;
	}

	public Pattern getFurtherPagesPattern() {
		return this.furtherPagesPattern;
	}

	public Pattern getTemplateRemovePattern() {
		return this.templateRemovePattern;
	}

	public Pattern getLinksFindPattern() {
		return this.linksFindPattern;
	}

	public String mergeCommaSeparatedNumbers(String text) {
		// merge comma separated numbers. Otherwise, e.g. "1,423" is tokenized
		// into "1" and "423"

		Matcher m = commaSeparatedNumbersPattern.matcher(text);

		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String newText = Matcher.quoteReplacement(m.group(1) + m.group(2));
			m.appendReplacement(sb, newText);
		}
		m.appendTail(sb);
		text = sb.toString();

		return text;
	}
}
