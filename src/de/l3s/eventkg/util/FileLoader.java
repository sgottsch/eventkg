package de.l3s.eventkg.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;

public class FileLoader {

	public static final String ONLINE_RESULTS_FOLDER_SUFFIX = "results/";
	public static final String ONLINE_RAW_DATA_FOLDER_SUFFIX = "raw_data/";
	public static final String ONLINE_META_FOLDER_SUFFIX = "meta/";
	public static final String ONLINE_OUTPUT_FOLDER_SUFFIX = "output/";
	public static final String ONLINE_OUTPUT_PREVIEW_FOLDER_SUFFIX = "output_preview/";
	public static final String ONLINE_OUTPUT_LIGHT_FOLDER_SUFFIX = "output_light/";
	public static final String ONLINE_OUTPUT_LIGHT_PREVIEW_FOLDER_SUFFIX = "output_light_preview/";
	public static final String ONLINE_PREVIOUS_VERSION_FOLDER_SUFFIX = "previous_version/";

	public static SimpleDateFormat PARSE_DATE_FORMAT = new SimpleDateFormat("G yyyy-MM-dd", Locale.US);
	public static SimpleDateFormat PRINT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public static String getFileNameWithPath(FileName fileName) {
		return getPath(fileName);
	}

	public static String getFileNameWithPath(FileName fileName, Language language) {
		return getPath(fileName, language);
	}

	public static File getFile(FileName fileName, Language language) {
		return new File(getPath(fileName, language));
	}

	public static File getFile(FileName fileName) {
		return new File(getPath(fileName));
	}

	public static File getFileLight(FileName fileName) {
		return new File(getLightPath(getPath(fileName)));
	}

	public static BufferedReader getReader(FileName fileName) throws FileNotFoundException {
		return getReader(getPath(fileName), fileName.hasColumnNamesInFirstLine());
	}

	public static BufferedReader getReader(String path, boolean hasColumnNamesInFirstLine)
			throws FileNotFoundException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		if (hasColumnNamesInFirstLine) {
			try {
				br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return br;
	}

	public static LineIterator getLineIteratorLight(FileName fileName) throws IOException {
		return getLineIterator(getLightPath(getPath(fileName)), fileName.hasColumnNamesInFirstLine());
	}

	public static LineIterator getLineIterator(FileName fileName) throws IOException {
		return getLineIterator(getPath(fileName), fileName.hasColumnNamesInFirstLine());
	}

	public static LineIterator getLineIterator(FileName fileName, Language language) throws IOException {
		return getLineIterator(getPath(fileName, language), fileName.hasColumnNamesInFirstLine());
	}

	public static LineIterator getLineIterator(String path, boolean hasColumnNamesInFirstLine) throws IOException {

		LineIterator it = FileUtils.lineIterator(new File(path), "UTF-8");

		if (hasColumnNamesInFirstLine)
			it.nextLine();

		return it;
	}

	public static List<String> readLines(FileName fileName) {

		List<String> lines = new ArrayList<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return lines;
	}

	public static List<String> readLines(FileName fileName, Language language) {

		List<String> lines = new ArrayList<String>();

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return lines;
	}

	public static BufferedReader getReader(FileName fileName, Language language) throws IOException {
		BufferedReader br = null;
		if (fileName.isGZipped())
			br = new BufferedReader(
					new InputStreamReader(new GZIPInputStream(new FileInputStream(getPath(fileName, language)))));
		else
			br = new BufferedReader(new InputStreamReader(new FileInputStream(getPath(fileName, language))));

		if (fileName.hasColumnNamesInFirstLine()) {
			try {
				br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return br;
	}

	public static PrintWriter getWriter(FileName fileName, Language language) throws FileNotFoundException {
		return new PrintWriter(getPath(fileName, language));
	}

	public static PrintWriter getWriter(FileName fileName) throws FileNotFoundException {
		return new PrintWriter(getPath(fileName));
	}

	public static PrintWriter getWriterLight(FileName fileName) throws FileNotFoundException {
		return new PrintWriter(getLightPath(getPath(fileName)));
	}

	public static PrintWriter getWriterWithAppend(FileName fileName) throws IOException {
		return new PrintWriter(new FileWriter(getPath(fileName), true));
	}

	public static PrintStream getPrintStream(FileName fileName) throws IOException {
		return new PrintStream(new FileOutputStream(getPath(fileName)));
	}

	public static String getPath(FileName fileName) {
		return getPath(fileName, null);
	}

	public static String getPath(FileName fileName, Language language) {

		// boolean local = true;
		// if (!new File(LOCAL_RESULTS_FOLDER).exists())
		// boolean local = false;

		String path = null;

		// if (local) {
		// if (fileName.isRawData())
		// path = LOCAL_RAW_DATA_FOLDER;
		// else if (fileName.isResultsData())
		// path = LOCAL_RESULTS_FOLDER;
		// else if (fileName.isMetaData())
		// path = LOCAL_META_FOLDER;
		// } else {
		// if (fileName.isRawData())
		// path = ONLINE_RAW_DATA_FOLDER;
		// else if (fileName.isResultsData())
		// path = ONLINE_RESULTS_FOLDER;
		// else if (fileName.isMetaData())
		// path = ONLINE_META_FOLDER;
		// }

		if (fileName.isRawData())
			path = Config.getValue("data_folder") + ONLINE_RAW_DATA_FOLDER_SUFFIX;
		else if (fileName.isResultsData())
			path = Config.getValue("data_folder") + ONLINE_RESULTS_FOLDER_SUFFIX;
		else if (fileName.isMetaData())
			path = Config.getValue("data_folder") + ONLINE_META_FOLDER_SUFFIX;
		else if (fileName.isOutputData())
			path = Config.getValue("data_folder") + ONLINE_OUTPUT_FOLDER_SUFFIX;
		else if (fileName.isOutputPreviewData())
			path = Config.getValue("data_folder") + ONLINE_OUTPUT_PREVIEW_FOLDER_SUFFIX;
		else if (fileName.isOutputLightData())
			path = Config.getValue("data_folder") + ONLINE_OUTPUT_LIGHT_FOLDER_SUFFIX;
		else if (fileName.isOutputLightPreviewData())
			path = Config.getValue("data_folder") + ONLINE_OUTPUT_LIGHT_PREVIEW_FOLDER_SUFFIX;
		else if (fileName.isPreviousVersionData())
			path = Config.getValue("data_folder") + ONLINE_PREVIOUS_VERSION_FOLDER_SUFFIX;

		if (fileName.getSource() != null) {
			if (language == null) {
				path = path + fileName.getSource().name().toLowerCase() + "/";
			} else {
				path = path + fileName.getSource().name().toLowerCase() + "/" + language.getLanguage() + "/";
			}
		}

		if (!fileName.isFolder()) {
			String fileNameString = fileName.getFileName();
			if (language != null && fileNameString.contains("$lang$"))
				fileNameString = fileNameString.replace("$lang$", language.getLanguage().toLowerCase());

			path = path + fileNameString;
			System.out.println("   Path: " + path);
			return path;
		} else {
			System.out.println("   Path: " + path);
			return path;
		}
	}

	public static List<File> getFilesList(FileName folderName, Language language) {

		if (!folderName.isFolder())
			throw new IllegalArgumentException("Folder expected, file given: " + folderName.getFileName() + ".");

		File dir = new File(getPath(folderName, language));

		File[] directoryListing = dir.listFiles();
		List<File> directoryListingWithPrefix = new ArrayList<File>();

		for (File file : directoryListing) {
			if (file.getName().startsWith(folderName.getFileName())) {
				directoryListingWithPrefix.add(file);
			}
		}

		return directoryListingWithPrefix;
	}

	public static List<File> getFilesList(FileName folderName) {

		if (!folderName.isFolder())
			throw new IllegalArgumentException("Folder expected, file given: " + folderName.getFileName() + ".");

		File dir = new File(getPath(folderName));

		File[] directoryListing = dir.listFiles();
		List<File> directoryListingWithPrefix = new ArrayList<File>();

		for (File file : directoryListing) {
			if (file.getName().startsWith(folderName.getFileName())) {
				directoryListingWithPrefix.add(file);
			}
		}

		return directoryListingWithPrefix;
	}

	public static PrintStream getPrintStream(FileName fileName, Language language) throws IOException {
		return new PrintStream(new FileOutputStream(getPath(fileName, language)));
	}

	public static String readFile(FileName fileName) throws IOException {
		return readFile(new File(getPath(fileName)));
	}

	public static String readFile(File file) throws IOException {

		String path = file.getAbsolutePath();

		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded);
	}

	public static boolean fileExists(FileName fileName, Language language) {
		String path = getPath(fileName, language);
		File f = new File(path);
		return f.exists();
	}

	public static boolean fileExists(FileName fileName) {
		String path = getPath(fileName);
		File f = new File(path);
		return f.exists();
	}

	public static String getLightPath(String path) {
		path = path.replace(ONLINE_OUTPUT_PREVIEW_FOLDER_SUFFIX, ONLINE_OUTPUT_LIGHT_PREVIEW_FOLDER_SUFFIX);
		path = path.replace(ONLINE_OUTPUT_FOLDER_SUFFIX, ONLINE_OUTPUT_LIGHT_FOLDER_SUFFIX);
		path = path.replace(".nq", ".nt");
		System.out.println("   Light path: " + path);
		return path;
	}

}
