package de.l3s.eventkg.pipeline.output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import de.l3s.eventkg.util.FileLoader;

public class RDFWriterStore {

	private Map<RDFWriterName, RDFWriter> writers;

	public RDFWriterStore() {
		this.writers = new HashMap<RDFWriterName, RDFWriter>();
	}

	public RDFWriter getWriter(RDFWriterName name) {
		RDFWriter writer = writers.get(name);

		if (writer == null)
			writer = initWriter(name);

		return writer;
	}

	private RDFWriter initWriter(RDFWriterName name) {

		System.out.println("Init writer " + name + ".");

		RDFWriter writer = new RDFWriter();
		writers.put(name, writer);

		// If file exists: open writers in append mode. Otherwise: create new
		// files.

		File file = FileLoader.getFile(name.getFileName());
		File fileLight = null;
		if (name.hasLightFile())
			fileLight = FileLoader.getFileLight(name.getFileName());

		File filePreview = null;
		File filePreviewLight = null;
		if (name.getFileNamePreview() != null) {
			filePreview = FileLoader.getFile(name.getFileNamePreview());
			if (name.hasLightFile())
				filePreviewLight = FileLoader.getFileLight(name.getFileNamePreview());
		}

		if (file.exists()) {
			// append!
			System.out.println("Append to files of " + name + ".");
			try {
				writer.setWriter(new PrintWriter(new FileOutputStream(file, true)));
				if (filePreview != null)
					writer.setWriterPreview(new PrintWriter(new FileOutputStream(filePreview, true)));
				if (fileLight != null)
					writer.setWriterLight(new PrintWriter(new FileOutputStream(fileLight, true)));
				if (filePreviewLight != null)
					writer.setWriterPreviewLight(new PrintWriter(new FileOutputStream(filePreviewLight, true)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Create files for " + name + ".");
			try {
				writer.setWriter(new PrintWriter(file));
				if (filePreview != null)
					writer.setWriterPreview(new PrintWriter(filePreview));
				if (fileLight != null)
					writer.setWriterLight(new PrintWriter(fileLight));
				if (filePreviewLight != null)
					writer.setWriterPreviewLight(new PrintWriter(filePreviewLight));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return writer;
	}

	public void closeWriters() {
		for (RDFWriter writer : writers.values()) {
			if (writer != null)
				writer.close();
		}
	}

}
