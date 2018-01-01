package de.l3s.eventkg.source.wikipedia;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class DumpFileCorrector {

	public static void main(String[] args) {
		String fileName = "/home/gottschalk/eventkb/eswc/data/raw_data/wikipedia/en/enwiki-20171201-pages-meta-current27.xml-p53163462p54663462.bz2";

		int lineNoGiven = 47939482;
//		int lineNoGiven = 5000;

		BufferedReader br = null;
		PrintWriter resWriter = null;

		try {
			try {
				resWriter = new PrintWriter("res.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			try {
				br = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(fileName))));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String line;
			int lineNo = 0;
			boolean deleteAllInPage = false;

			try {
				while ((line = br.readLine()) != null) {
					lineNo += 1;

					String trimmedLine = line.trim();

					if (deleteAllInPage && !trimmedLine.equals("</page>")) {

					} else if (deleteAllInPage && trimmedLine.equals("</page>")) {
						deleteAllInPage = false;
					} else if (trimmedLine.equals("<page>") && lineNo >= (lineNoGiven - 1000)
							&& lineNo <= (lineNoGiven + 1000)) {
						deleteAllInPage = true;
						System.out.println("Delete page in line " + lineNo);
					} else {
						resWriter.write(line + "\n");
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			resWriter.close();
		}

	}

}
