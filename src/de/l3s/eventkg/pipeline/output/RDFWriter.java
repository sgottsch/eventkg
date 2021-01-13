package de.l3s.eventkg.pipeline.output;

import java.io.PrintWriter;

public class RDFWriter {

	public static final int MAX_NUMBER_OF_INSTANCES_IN_PREVIEW = 30;

	private PrintWriter writer;
	private PrintWriter writerLight;

	private PrintWriter writerPreview;
	private PrintWriter writerPreviewLight;

	private boolean isInitiated;

	private int numberOfLinesPreview = 0;
	private int numberOfLinesPreviewLight = 0;

	public PrintWriter getWriter() {
		return writer;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	public PrintWriter getWriterLight() {
		return writerLight;
	}

	public void setWriterLight(PrintWriter writerLight) {
		this.writerLight = writerLight;
	}

	public PrintWriter getWriterPreview() {
		return writerPreview;
	}

	public void setWriterPreview(PrintWriter writerPreview) {
		this.writerPreview = writerPreview;
	}

	public PrintWriter getWriterPreviewLight() {
		return writerPreviewLight;
	}

	public void setWriterPreviewLight(PrintWriter writerPreviewLight) {
		this.writerPreviewLight = writerPreviewLight;
	}

	public boolean isInitiated() {
		return isInitiated;
	}

	public void setInitiated(boolean isInitiated) {
		this.isInitiated = isInitiated;
	}

	public void resetNumberOfLines() {
		numberOfLinesPreviewLight = 0;
		numberOfLinesPreview = 0;
	}

	public boolean previewIsValid() {

		if (writerPreview == null)
			return false;

		if (numberOfLinesPreview < MAX_NUMBER_OF_INSTANCES_IN_PREVIEW)
			return true;
		else
			return false;
	}

	public boolean previewLightIsValid() {
		if (numberOfLinesPreviewLight < MAX_NUMBER_OF_INSTANCES_IN_PREVIEW)
			return true;
		else
			return false;
	}

	public void write(String line) {
		this.writer.println(line);
		if (previewIsValid())
			this.writerPreview.println(line);

		this.writerLight.println(line);
		if (previewIsValid())
			this.writerPreviewLight.println(line);
	}

	public void write(LinePair line, boolean printLight) {
		this.writer.println(line.getLine());
		
		if (previewIsValid())
			this.writerPreview.println(line.getLine());

		if (printLight) {
			this.writerLight.println(line.getLineLight());
			if (previewIsValid())
				this.writerPreviewLight.println(line.getLineLight());
		}
	}

	public void writeLight(String line) {
		this.writerLight.println(line);
		if (previewLightIsValid())
			this.writerPreviewLight.println(line);
	}

	public void close() {
		this.writer.close();
		if (this.writerPreview != null)
			this.writerPreview.close();
		if (this.writerLight != null)
			this.writerLight.close();
		if (this.writerPreviewLight != null)
			this.writerPreviewLight.close();
	}

	public void increasePreviewLineCount() {
		this.numberOfLinesPreview += 1;
	}

	public void increasePreviewLineCountLight() {
		this.numberOfLinesPreviewLight += 1;
	}

}
