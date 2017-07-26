/*
 * MediaWiki import/export processing tools
 * Copyright 2005 by Brion Vibber
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * $Id$
 */

/*
	-> read header info
	site name, url, language, namespace keys
	
	-> read pages.....
	<page>
		-> get title, etc
		<revision>
			-> store each revision
			on next one or end of sequence, write out
			[so for 1.4 schema we can be friendly]
	
	progress report: [TODO]
		if possible, a percentage through file. this might not be possible.
		rates and counts definitely
	
	input:
		stdin or file
		gzip and bzip2 decompression on files with standard extensions
	
	output:
		stdout
		file
		gzip file
		bzip2 file
		future: SQL directly to a server?
	
	output formats:
		XML export format 0.3
		1.4 SQL schema
		1.5 SQL schema
		
*/

package org.mediawiki.dumper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.mediawiki.importer.AfterTimeStampFilter;
import org.mediawiki.importer.BeforeTimeStampFilter;
import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.ExactListFilter;
import org.mediawiki.importer.LatestFilter;
import org.mediawiki.importer.ListFilter;
import org.mediawiki.importer.MultiWriter;
import org.mediawiki.importer.NamespaceFilter;
import org.mediawiki.importer.NotalkFilter;
import org.mediawiki.importer.RevisionListFilter;
import org.mediawiki.importer.TitleMatchFilter;
import org.mediawiki.importer.XmlDumpReader;
import org.mediawiki.importer.XmlDumpWriter0_10;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.wikipedia.RedirectsTableCreator;
import de.l3s.eventkg.wikipedia.mwdumper.articleprocessing.EventKGExtractor;

class Dumper {

	// TODO: Cut it to the actual needs of EventKG. Integrate it in the
	// pipeline. Put language as parameter. Just use the existing MWDumper maven
	// repo and wrap own stuff around it.

	public static void main(String[] args) throws IOException, ParseException {
		InputStream input = null;
		OutputWrapper output = null;
		DumpWriter sink = null;
		MultiWriter writers = new MultiWriter();
		int progressInterval = 1000;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String[] bits = splitArg(arg);
			if (bits != null) {
				String opt = bits[0], val = bits[1], param = bits[2];
				if (opt.equals("output")) {
					if (output != null) {
						// Finish constructing the previous output...
						if (sink == null)
							sink = new XmlDumpWriter0_10(output.getFileStream());
						writers.add(sink);
						sink = null;
					}
					output = openOutputFile(val, param);
				} else if (opt.equals("format")) {
					if (output == null)
						output = new OutputWrapper(Tools.openStandardOutput());
					if (sink != null)
						throw new IllegalArgumentException("Only one format per output allowed.");
					sink = openOutputSink(output, val, param);
				} else if (opt.equals("filter")) {
					if (sink == null) {
						if (output == null)
							output = new OutputWrapper(Tools.openStandardOutput());
						sink = new XmlDumpWriter0_10(output.getFileStream());
					}
					sink = addFilter(sink, val, param);
				} else if (opt.equals("progress")) {
					progressInterval = Integer.parseInt(val);
				} else if (opt.equals("quiet")) {
					progressInterval = 0;
				} else {
					throw new IllegalArgumentException("Unrecognized option " + opt);
				}
			} else if (arg.equals("-")) {
				if (input != null)
					throw new IllegalArgumentException("Input already set; can't set to stdin");
				input = Tools.openStandardInput();
			} else {
				if (input != null)
					throw new IllegalArgumentException("Input already set; can't set to " + arg);
				input = Tools.openInputFile(arg);
			}
		}

		if (input == null)
			input = Tools.openStandardInput();
		if (output == null)
			output = new OutputWrapper(Tools.openStandardOutput());
		// Finish stacking the last output sink
		if (sink == null)
			sink = new XmlDumpWriter0_10(output.getFileStream());
		writers.add(sink);

		DumpWriter outputSink = (progressInterval > 0) ? (DumpWriter) new ProgressFilter(writers, progressInterval)
				: (DumpWriter) writers;

		XmlDumpReader reader = new XmlDumpReader(input, outputSink);
		reader.readDump();
	}

	/**
	 * @param arg
	 *            string in format "--option=value:parameter"
	 * @return array of option, value, and parameter, or null if no match
	 */
	static String[] splitArg(String arg) {
		if (!arg.startsWith("--"))
			return null;

		String opt = "";
		String val = "";
		String param = "";

		String[] bits = arg.substring(2).split("=", 2);
		opt = bits[0];

		if (bits.length > 1) {
			String[] bits2 = bits[1].split(":", 2);
			val = bits2[0];
			if (bits2.length > 1)
				param = bits2[1];
		}

		return new String[] { opt, val, param };
	}

	// ----------------

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

	static OutputWrapper openOutputFile(String dest, String param) throws IOException {
		if (dest.equals("stdout"))
			return new OutputWrapper(Tools.openStandardOutput());
		else if (dest.equals("file"))
			return new OutputWrapper(Tools.createOutputFile(param));
		else if (dest.equals("gzip"))
			return new OutputWrapper(new GZIPOutputStream(Tools.createOutputFile(param)));
		else if (dest.equals("bzip2"))
			return new OutputWrapper(Tools.createBZip2File(param));
		else
			throw new IllegalArgumentException("Destination sink not implemented: " + dest);
	}

	static DumpWriter openOutputSink(OutputWrapper output, String format, String param) throws IOException {
		if (format.equals("eventkg")) {

			// TODO: Do that somewhere else
			Config.init("config_eventkb.txt");

			BufferedWriter fileEvents = null;
			BufferedWriter fileFirstSentences = null;
			BufferedWriter fileLinkSets = null;
			BufferedWriter fileLinkCounts = null;
			Language language = Language.FR;

			Map<String, String> redirects = RedirectsTableCreator.getRedirects(language);

			try {
				Date currentDate = new Date();
				Random random = new Random();
				long LOWER_RANGE = 0L;
				long UPPER_RANGE = 999999999L;
				long randomValue = LOWER_RANGE + (long) (random.nextDouble() * (UPPER_RANGE - LOWER_RANGE));
				String pathSuffix = language + "_" + new SimpleDateFormat("yyyyMMddhhmm").format(new Date()) + "_"
						+ currentDate.getTime() + "_" + String.valueOf(randomValue) + ".txt";
				String path1 = "pages/events-" + pathSuffix;
				String path2 = "pages/first_sentences-" + pathSuffix;
				String path3 = "pages/link_sets-" + pathSuffix;
				String path4 = "pages/link_counts-" + pathSuffix;
				System.out.println(path1);
				fileEvents = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path1, false), "UTF-8"));
				fileFirstSentences = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(path2, false), "UTF-8"));
				fileLinkSets = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path3, false), "UTF-8"));
				fileLinkCounts = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(path4, false), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// TODO: Configure language in params
			return new EventKGExtractor(param, language, fileEvents, fileFirstSentences, fileLinkSets, fileLinkCounts,
					redirects);
		} else {
			throw new IllegalArgumentException("Output format not known: " + format);
		}
	}

	static DumpWriter addFilter(DumpWriter sink, String filter, String param) throws IOException, ParseException {
		if (filter.equals("latest"))
			return new LatestFilter(sink);
		else if (filter.equals("namespace"))
			return new NamespaceFilter(sink, param);
		else if (filter.equals("notalk"))
			return new NotalkFilter(sink);
		else if (filter.equals("titlematch"))
			return new TitleMatchFilter(sink, param);
		else if (filter.equals("list"))
			return new ListFilter(sink, param);
		else if (filter.equals("exactlist"))
			return new ExactListFilter(sink, param);
		else if (filter.equals("revlist"))
			return new RevisionListFilter(sink, param);
		else if (filter.equals("before"))
			return new BeforeTimeStampFilter(sink, param);
		else if (filter.equals("after"))
			return new AfterTimeStampFilter(sink, param);
		else
			throw new IllegalArgumentException("Filter unknown: " + filter);
	}
}
