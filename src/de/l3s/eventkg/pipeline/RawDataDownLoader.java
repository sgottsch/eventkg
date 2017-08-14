package de.l3s.eventkg.pipeline;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class RawDataDownLoader {

	private String dataPath;

	private List<Language> languages;

	private String metaDataPath;

	public void copyMetaFiles() {

		System.out.println("Copy meta files.");
		// Currently, we only have meta files for Wikidata

		try {
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/"
							+ FileName.WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES.getFileName()),
					new File(metaDataPath + "wikidata/"
							+ FileName.WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/"
							+ FileName.WIKIDATA_EXTERNAL_IDS_PROPERTY_NAMES.getFileName()),
					new File(metaDataPath + "wikidata/" + FileName.WIKIDATA_EXTERNAL_IDS_PROPERTY_NAMES.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource(
							"/resource/meta_data/wikidata/" + FileName.WIKIDATA_LOCATION_PROPERTY_NAMES.getFileName()),
					new File(metaDataPath + "wikidata/" + FileName.WIKIDATA_LOCATION_PROPERTY_NAMES.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/"
							+ FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME.getFileName()),
					new File(metaDataPath + "wikidata/"
							+ FileName.WIKIDATA_TEMPORAL_PROPERTY_LIST_FILE_NAME.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/"
							+ FileName.WIKIDATA_SUB_LOCATION_PROPERTY_NAMES.getFileName()),
					new File(metaDataPath + "wikidata/" + FileName.WIKIDATA_SUB_LOCATION_PROPERTY_NAMES.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class
							.getResource("/resource/meta_data/yago/" + FileName.YAGO_TIME_PROPERTIES.getFileName()),
					new File(metaDataPath + "yago/" + FileName.YAGO_TIME_PROPERTIES.getFileName()));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public RawDataDownLoader(List<Language> languages) {
		this.languages = languages;
	}

	void createFolders() {

		// String dataPath = FileLoader.LOCAL_RAW_DATA_FOLDER;
		this.dataPath = Config.getValue("data_folder");

		(new File(dataPath + "raw_data/wce/")).mkdirs();
		(new File(dataPath + "raw_data/yago/")).mkdirs();
		(new File(dataPath + "raw_data/wikidata/")).mkdirs();
		(new File(dataPath + "raw_data/wce/")).mkdirs();

		(new File(dataPath + "meta/wce/")).mkdirs();
		(new File(dataPath + "meta/yago/")).mkdirs();
		(new File(dataPath + "meta/wikidata/")).mkdirs();
		(new File(dataPath + "meta/wce/")).mkdirs();
		(new File(dataPath + "meta/all/")).mkdirs();

		(new File(dataPath + "results/wce/")).mkdirs();
		(new File(dataPath + "results/yago/")).mkdirs();
		(new File(dataPath + "results/wce/")).mkdirs();
		(new File(dataPath + "results/all/")).mkdirs();

		for (Language language : languages) {
			(new File(dataPath + "raw_data/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "meta/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "raw_data/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "meta/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/wikidata/" + language.getLanguage())).mkdirs();
		}

		this.metaDataPath = this.dataPath + FileLoader.ONLINE_META_FOLDER_SUFFIX;
		this.dataPath = this.dataPath + FileLoader.ONLINE_RAW_DATA_FOLDER_SUFFIX;
	}

	void downloadFiles() {
		downloadWikipediaFiles();
		downloadDBPediaFiles();
		downloadWCEFiles();
		downloadYAGOFiles();
		downloadWikidataFile();
	}

	private void downloadWikipediaFiles() {

		for (Language language : this.languages) {

			String wikiName = language.getWiki();
			String dumpDate = Config.getValue(wikiName);

			Writer dumpFilesListFile = null;
			String baseUrl = "https://dumps.wikimedia.org/" + wikiName + "/" + dumpDate + "/";

			try {
				dumpFilesListFile = FileLoader.getWriter(FileName.WIKIPEDIA_DUMP_FILE_LIST, language);

				Document doc = Jsoup.connect(baseUrl).get();
				Elements links = doc.select("a[href]");

				for (Element link : links) {
					String url = link.attr("href");
					url = url.substring(1);

					if (url.contains("pages-meta-current") && url.endsWith(".bz2")
							&& !url.endsWith("pages-meta-current.xml.bz2")) {
						downloadFile(baseUrl + url.substring(url.lastIndexOf("/")),
								FileLoader.getPath(FileName.WIKIPEDIA_DUMPS, language) + "/"
										+ url.substring(url.lastIndexOf("/")));
						// dumpFilesListFile.write(url.substring(url.lastIndexOf("/"))
						// + "\n");
						dumpFilesListFile.write(FileLoader.getPath(FileName.WIKIPEDIA_DUMPS, language).toString()
								+ url.substring(url.lastIndexOf("/")) + "\n");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					dumpFilesListFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			downloadFile(baseUrl + wikiName + "-" + dumpDate + "-redirect.sql.gz",
					FileLoader.getFileNameWithPath(FileName.WIKIPEDIA_REDIRECTS, language));
			downloadFile(baseUrl + wikiName + "-" + dumpDate + "-page.sql.gz",
					FileLoader.getFileNameWithPath(FileName.WIKIPEDIA_PAGE_INFOS, language));
			downloadFile(baseUrl + wikiName + "-" + dumpDate + "-categorylinks.sql.gz",
					FileLoader.getFileNameWithPath(FileName.WIKIPEDIA_CATEGORYLINKS, language));
		}

	}

	private void downloadWCEFiles() {
		String url = "http://wikitimes.l3s.de/webresources/WebService/getEvents/json/$yyyy$-01-01/$yyyy$-12-31";

		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		for (int year = 2000; year <= currentYear; year++) {
			downloadFile(url.replaceAll("\\$yyyy\\$", String.valueOf(year)),
					this.dataPath + "wce/events_" + String.valueOf(year) + ".txt");
		}
	}

	private void downloadWikidataFile() {

		this.dataPath = Config.getValue("data_folder");
		this.metaDataPath = this.dataPath + FileLoader.ONLINE_META_FOLDER_SUFFIX;
		this.dataPath = this.dataPath + FileLoader.ONLINE_RAW_DATA_FOLDER_SUFFIX;

		downloadFile("https://dumps.wikimedia.org/wikidatawiki/entities/" + Config.getValue("wikidata") + "/wikidata-"
				+ Config.getValue("wikidata") + "-all.json.gz", this.dataPath + "wikidata/dump.json.gz");
	}

	public void downloadDBPediaFiles() {

		String dbPediaUrl = "http://downloads.dbpedia.org/" + Config.getValue("dbpedia") + "/core-i18n/$lang$/";
		Set<String> urls = new HashSet<String>();
		urls.add("instance_types_transitive_$lang$.ttl.bz2");
		urls.add("mappingbased_objects_$lang$.ttl.bz2");
		urls.add("mappingbased_literals_$lang$.ttl.bz2");

		for (Language language : this.languages) {
			for (String urlString : urls) {

				urlString = urlString.replaceAll("\\$lang\\$", language.getLanguage());
				String fileNameBZ = this.dataPath + "dbpedia/" + language.getLanguage() + "/" + urlString;
				String fileName = (this.dataPath + "dbpedia/" + language.getLanguage() + "/" + urlString)
						.replaceAll("\\.bz2$", "");

				File downloadedFile = downloadFile(
						dbPediaUrl.replaceAll("\\$lang\\$", language.getLanguage()) + urlString, fileNameBZ);

				FileInputStream fin = null;
				FileOutputStream out = null;
				BZip2CompressorInputStream bzIn = null;
				try {
					fin = new FileInputStream(fileNameBZ);
					BufferedInputStream in = new BufferedInputStream(fin);
					out = new FileOutputStream(fileName);
					bzIn = new BZip2CompressorInputStream(in);
					final byte[] buffer = new byte[1024];
					int n = 0;
					while (-1 != (n = bzIn.read(buffer))) {
						out.write(buffer, 0, n);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						fin.close();
						out.close();
						bzIn.close();
						Files.delete(downloadedFile.toPath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

		}

	}

	public void downloadYAGOFiles() {

		String yagoUrl = "http://resources.mpi-inf.mpg.de/yago-naga/yago/download/yago/";
		Set<String> urls = new HashSet<String>();
		urls.add("yagoTaxonomy.ttl.7z");
		urls.add("yagoDateFacts.ttl.7z");
		urls.add("yagoFacts.ttl.7z");
		urls.add("yagoMetaFacts.ttl.7z");

		for (String urlString : urls) {
			File downloadedFile = null;

			try {

				downloadedFile = downloadFile(yagoUrl + urlString, this.dataPath + "yago/" + urlString);

				InputStream inputStream = new FileInputStream(this.dataPath + "yago/" + urlString);

				SevenZFile sevenZFile = null;

				try {
					sevenZFile = new SevenZFile(downloadedFile);
					SevenZArchiveEntry entry = sevenZFile.getNextEntry();
					while (entry != null) {
						OutputStream out = new FileOutputStream(this.dataPath + "yago/" + entry.getName());
						byte[] content = new byte[(int) entry.getSize()];
						sevenZFile.read(content, 0, content.length);
						out.write(content);
						out.close();
						entry = sevenZFile.getNextEntry();
					}
					sevenZFile.close();
					// outputStream.close();
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					Files.delete(downloadedFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private File downloadFile(String url, String targetPath) {

		URL website = null;
		try {
			website = new URL(url);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		FileOutputStream fos = null;

		boolean succ = false;
		while (!succ) {
			System.out.println("Download file " + url + " to " + targetPath + ".");

			try {
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				fos = new FileOutputStream(targetPath);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				succ = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				if (e.getMessage().contains("response code: 503")) {
					// if server is overload: wait for 5 seconds and re-try
					System.out.println(e.getMessage());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				} else
					e.printStackTrace();
			} finally {
				try {
					if (fos != null)
						fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("=> Done.");

		return new File(targetPath);
	}

}
