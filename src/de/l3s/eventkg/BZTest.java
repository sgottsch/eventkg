package de.l3s.eventkg;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class BZTest {

	public static void main(String[] args) throws IOException {

		List<String> lines = FileUtils.readLines(
				new File("/home/simon/Documents/EventKB/enwiki-20171201-pages-meta-current27.xml-p53163462p54663462"));

		
	}

}
