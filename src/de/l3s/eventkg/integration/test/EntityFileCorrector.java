package de.l3s.eventkg.integration.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EntityFileCorrector {

	public static void main(String[] args) {

		Config.init(args[0]);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter("abcd.nq");

			LineIterator it = null;
			try {
				it = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_WITH_TEXTS);
				while (it.hasNext()) {
					String line = it.nextLine();

					boolean valid = true;

					if (line.contains("<http://www.w3.org/2002/07/owl#sameAs>")) {
						String object = line.substring(line.indexOf(" ") + 1);
						object = object.substring(object.indexOf(" ") + 1);
						object = object.substring(0, object.lastIndexOf(" "));
						object = object.substring(0, object.lastIndexOf(" "));
						if (object.contains(" "))
							valid = false;
					}

					if (valid)
						writer.write(line + "\n");
					else
						System.out.println("Ignore line: " + line);

				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					it.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			writer.close();
		}

	}

}
