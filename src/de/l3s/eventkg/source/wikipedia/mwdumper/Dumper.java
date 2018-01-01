package de.l3s.eventkg.source.wikipedia.mwdumper;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.mediawiki.dumper.ProgressFilter;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.MultiWriter;
import org.mediawiki.importer.XmlDumpReader;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.source.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.source.wikipedia.WikiWords;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.EventDateExpressionsAll;
import de.l3s.eventkg.source.wikipedia.mwdumper.articleprocessing.EventKGExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

class Dumper {

	private static final int IN_BUF_SZ = 1024 * 1024;

	public static void main(String[] args) throws IOException, ParseException {

		if (args.length != 2)
			return;

		Config.init(args[0]);
		MultiWriter writers = new MultiWriter();

		Language language = Language.getLanguage(args[1]);

		List<Language> languages = new ArrayList<Language>();
		languages.add(language);
		WikiWords.getInstance().init(languages);
		EventDateExpressionsAll.getInstance().init(language);

		DumpWriter sink = getEventKGExtractor(language);
		InputStream input = openStandardInput();

		writers.add(sink);

		int progressInterval = 1000;
		DumpWriter outputSink = (progressInterval > 0) ? (DumpWriter) new ProgressFilter(writers, progressInterval)
				: (DumpWriter) writers;

		XmlDumpReader reader = new XmlDumpReader(input, outputSink);
		reader.readDump();
	}

	static InputStream openStandardInput() throws IOException {
		return new BufferedInputStream(System.in, IN_BUF_SZ);
	}

	static class OutputWrapper {
		private OutputStream fileStream = null;
		private Connection sqlConnection = null;

		OutputWrapper(OutputStream aFileStream) {
			fileStream = aFileStream;
		}

		OutputWrapper(Connection anSqlConnection) {
			sqlConnection = anSqlConnection;
		}

		OutputStream getFileStream() {
			if (fileStream != null)
				return fileStream;
			if (sqlConnection != null)
				throw new IllegalArgumentException("Expected file stream, got SQL connection?");
			throw new IllegalArgumentException("Have neither file nor SQL connection. Very confused!");
		}

	}

	private static EventKGExtractor getEventKGExtractor(Language language) {
		BufferedWriter fileEvents = null;
		BufferedWriter fileFirstSentences = null;
		BufferedWriter fileLinkSets = null;
		BufferedWriter fileLinkCounts = null;

		Map<String, String> redirects = RedirectsTableCreator.getRedirects(language);
		
		try {
			Date currentDate = new Date();
			Random random = new Random();
			long LOWER_RANGE = 0L;
			long UPPER_RANGE = 999999999L;
			long randomValue = LOWER_RANGE + (long) (random.nextDouble() * (UPPER_RANGE - LOWER_RANGE));
			String pathSuffix = language + "_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date()) + "_"
					+ currentDate.getTime() + "_" + String.valueOf(randomValue) + ".txt";
			String path1 = FileLoader.getPath(FileName.WIKIPEDIA_TEXTUAL_EVENTS, language) + "/"
					+ FileName.WIKIPEDIA_TEXTUAL_EVENTS.getFileName() + pathSuffix;
			String path2 = FileLoader.getPath(FileName.WIKIPEDIA_FIRST_SENTENCES, language) + "/"
					+ FileName.WIKIPEDIA_FIRST_SENTENCES.getFileName() + pathSuffix;
			String path3 = FileLoader.getPath(FileName.WIKIPEDIA_LINK_SETS, language) + "/"
					+ FileName.WIKIPEDIA_LINK_SETS.getFileName() + pathSuffix;
			String path4 = FileLoader.getPath(FileName.WIKIPEDIA_LINK_COUNTS, language) + "/"
					+ FileName.WIKIPEDIA_LINK_COUNTS.getFileName() + pathSuffix;
			System.out.println(path1);
			fileEvents = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path1, false), "UTF-8"));
			fileFirstSentences = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(path2, false), "UTF-8"));
			fileLinkSets = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path3, false), "UTF-8"));
			fileLinkCounts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path4, false), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new EventKGExtractor("-1", language, fileEvents, fileFirstSentences, fileLinkSets, fileLinkCounts,
				redirects);
	}

}
