package de.l3s.eventkg.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.source.dbpedia.download.DBPediaDumpFilesFinder;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

public class RawDataDownLoader {

	private String dataPath;

	private List<Language> languages;

	private String metaDataPath;

	public void copyMetaFiles() {

		System.out.println("Copy meta files.");

		try {

			// Wikidata
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/"
							+ FileName.WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES.getFileName()),
					new File(metaDataPath + "wikidata/"
							+ FileName.WIKIDATA_MANUAL_FORBIDDEN_PROPERTY_NAMES.getFileName()));
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
					RawDataDownLoader.class.getResource(
							"/resource/meta_data/wikidata/" + FileName.WIKIDATA_EVENT_BLACKLIST_CLASSES.getFileName()),
					new File(metaDataPath + "wikidata/" + FileName.WIKIDATA_EVENT_BLACKLIST_CLASSES.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource(
							"/resource/meta_data/wikidata/" + FileName.WIKIDATA_IGNORED_EVENT_CLASSES.getFileName()),
					new File(metaDataPath + "wikidata/" + FileName.WIKIDATA_IGNORED_EVENT_CLASSES.getFileName()));

			// YAGO
			FileUtils.copyURLToFile(
					RawDataDownLoader.class
							.getResource("/resource/meta_data/yago/" + FileName.YAGO_TIME_PROPERTIES.getFileName()),
					new File(metaDataPath + "yago/" + FileName.YAGO_TIME_PROPERTIES.getFileName()));

			// DBpedia
			FileUtils.copyURLToFile(
					RawDataDownLoader.class.getResource(
							"/resource/meta_data/dbpedia/" + FileName.DBPEDIA_PART_OF_PROPERTIES.getFileName()),
					new File(metaDataPath + "dbpedia/" + FileName.DBPEDIA_PART_OF_PROPERTIES.getFileName()));

			// EventKG
			FileUtils.copyURLToFile(
					RawDataDownLoader.class
							.getResource("/resource/meta_data/event_kg/" + FileName.ALL_TTL_SCHEMA_INPUT.getFileName()),
					new File(metaDataPath + "event_kg/" + FileName.ALL_TTL_SCHEMA_INPUT.getFileName()));
			FileUtils.copyURLToFile(
					RawDataDownLoader.class
							.getResource("/resource/meta_data/event_kg/" + FileName.ALL_TTL_VOID_INPUT.getFileName()),
					new File(metaDataPath + "event_kg/" + FileName.ALL_TTL_VOID_INPUT.getFileName()));

			for (Language language : this.languages) {
				FileUtils.copyURLToFile(
						RawDataDownLoader.class
								.getResource("/resource/meta_data/wikipedia/" + language.getLanguageLowerCase() + "/"
										+ FileName.WIKIPEDIA_META_EVENT_DATE_EXPRESSIONS.getFileName()),
						new File(metaDataPath + "wikipedia/" + language.getLanguageLowerCase() + "/"
								+ FileName.WIKIPEDIA_META_EVENT_DATE_EXPRESSIONS.getFileName()));
				FileUtils.copyURLToFile(
						RawDataDownLoader.class.getResource("/resource/meta_data/wikipedia/"
								+ language.getLanguageLowerCase() + "/" + FileName.WIKIPEDIA_META_WORDS.getFileName()),
						new File(metaDataPath + "wikipedia/" + language.getLanguageLowerCase() + "/"
								+ FileName.WIKIPEDIA_META_WORDS.getFileName()));
			}

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
		String dbPath = Config.getValue("db_folder");

		(new File(dataPath + "raw_data/wce/")).mkdirs();
		(new File(dataPath + "raw_data/yago/")).mkdirs();
		(new File(dataPath + "raw_data/wikidata/")).mkdirs();
		(new File(dataPath + "raw_data/wce/")).mkdirs();
		(new File(dataPath + "raw_data/all/")).mkdirs();

		(new File(dataPath + "meta/wce/")).mkdirs();
		(new File(dataPath + "meta/yago/")).mkdirs();
		(new File(dataPath + "meta/wikidata/")).mkdirs();
		(new File(dataPath + "meta/wce/")).mkdirs();
		(new File(dataPath + "meta/dbpedia/")).mkdirs();
		(new File(dataPath + "meta/all/")).mkdirs();

		(new File(dataPath + "results/wce/")).mkdirs();
		(new File(dataPath + "results/yago/")).mkdirs();
		(new File(dataPath + "results/wce/")).mkdirs();
		(new File(dataPath + "results/all/")).mkdirs();

		(new File(dataPath + "output/")).mkdirs();
		(new File(dataPath + "output_preview/")).mkdirs();

		(new File(dataPath + "previous_version/")).mkdirs();

		for (Language language : languages) {
			(new File(dataPath + "raw_data/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "meta/dbpedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "raw_data/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "meta/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "results/wikidata/" + language.getLanguage())).mkdirs();
			(new File(dataPath + "meta/wikipedia/" + language.getLanguage())).mkdirs();
			(new File(dbPath + "/" + language.getLanguageLowerCase())).mkdirs();
			(new File(dbPath + "/eventkg_old/" + language.getLanguageLowerCase())).mkdirs();
			(new File(dbPath + "/eventkg_current/" + language.getLanguageLowerCase())).mkdirs();
		}
		(new File(dbPath + "/all")).mkdirs();

		this.metaDataPath = this.dataPath + FileLoader.ONLINE_META_FOLDER_SUFFIX;
		this.dataPath = this.dataPath + FileLoader.ONLINE_RAW_DATA_FOLDER_SUFFIX;
	}

	void downloadFiles() {

		this.dataPath = Config.getValue("data_folder");
		this.dataPath = this.dataPath + FileLoader.ONLINE_RAW_DATA_FOLDER_SUFFIX;

		downloadSEMOntology();
		downloadYAGOFiles();
		downloadWikipediaFiles();
		downloadDBPediaFiles();
		downloadWCEFiles();
		downloadWikidataFile();
	}

	private void downloadSEMOntology() {

		downloadFile("http://semanticweb.cs.vu.nl/2009/11/sem/sem.rdf",
				FileLoader.getFileNameWithPath(FileName.SEM_ONTOLOGY));

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

				// Big Wikipedias have singles files like
				// "itwiki-20190701-pages-meta-current2.xml-p277092p1057872.bz2"
				// and a combined file like
				// "itwiki-20190701-pages-meta-current.xml.bz2". For the dumper,
				// make sure that you download the single files and not the
				// combined one. Small Wikipedia have the combined file only,
				// but no single file. In that case make sure that the
				// combinedone is downloaded.

				boolean hasParts = false;
				for (Element link : links) {
					String url = link.attr("href");
					url = url.substring(1);
					if (url.contains("pages-meta-current") && url.endsWith(".bz2")
							&& !url.endsWith("pages-meta-current.xml.bz2")) {
						hasParts = true;
						break;
					}
				}

				for (Element link : links) {
					String url = link.attr("href");
					url = url.substring(1);

					if ((hasParts && (url.contains("pages-meta-current") && url.endsWith(".bz2")
							&& !url.endsWith("pages-meta-current.xml.bz2")))
							|| (!hasParts && url.endsWith("pages-meta-current.xml.bz2"))) {
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

		if (!languages.contains(Language.EN))
			return;

		// wikitimes is out of service!
		// String url =
		// "http://wikitimes.l3s.de/webresources/WebService/getEvents/json/$yyyy$-01-01/$yyyy$-12-31";
		//
		// int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		// for (int year = 2000; year <= currentYear; year++) {
		// downloadFile(url.replaceAll("\\$yyyy\\$", String.valueOf(year)),
		// this.dataPath + "wce/events_" + String.valueOf(year) + ".txt");
		// }

		List<String> monthNames = Arrays.asList("January", "February", "March", "April", "May", "June", "July",
				"August", "September", "October", "November", "December");

		String url = "https://en.wikipedia.org/w/api.php?action=parse&page=Portal:Current_events/$date$&action=parse&format=json";

		Calendar c = Calendar.getInstance();
		int currentYear = c.get(Calendar.YEAR);
		int currentMonth = c.get(Calendar.MONTH);

		for (int year = 1994; year <= currentYear; year++) {
			for (int month = 0; month <= 11; month++) {

				// the first WCE page is October 1994
				if (year == 1994 && month < 9)
					continue;

				if (year > currentYear)
					break;

				if (year == currentYear && month > currentMonth)
					break;

				downloadFile(url.replaceAll("\\$date\\$", monthNames.get(month) + "_" + year),
						this.dataPath + "wce/events_" + String.valueOf(year) + "_" + (month + 1) + ".json");

			}
		}

	}

	private void downloadWikidataFile() {

		downloadWikidataQueryFiles();

		this.dataPath = Config.getValue("data_folder");
		this.metaDataPath = this.dataPath + FileLoader.ONLINE_META_FOLDER_SUFFIX;
		this.dataPath = this.dataPath + FileLoader.ONLINE_RAW_DATA_FOLDER_SUFFIX;

		downloadFile("https://dumps.wikimedia.org/wikidatawiki/entities/" + Config.getValue("wikidata") + "/wikidata-"
				+ Config.getValue("wikidata") + "-all.json.gz", this.dataPath + "wikidata/dump.json.gz");
	}

	public static void downloadWikidataQueryFiles() {

		Map<FileName, String> queryFiles = new LinkedHashMap<FileName, String>();
		queryFiles.put(FileName.WIKIDATA_PROPERTY_EQUALITIES, "equivalence_properties_query.sparql");
		queryFiles.put(FileName.WIKIDATA_EXTERNAL_IDS_PROPERTY_NAMES, "properties_for_identifiers.sparql");
		queryFiles.put(FileName.WIKIDATA_WIKIPEDIA_INTERNAL_ITEMS, "wikipedia_internal_items.sparql");

		for (FileName fileName : queryFiles.keySet()) {
			String query;
			PrintWriter writer = null;
			try {
				System.out.println("Run query " + queryFiles.get(fileName) + ".");

				query = IOUtils.toString(
						RawDataDownLoader.class.getResource("/resource/meta_data/wikidata/" + queryFiles.get(fileName)),
						"UTF-8");

				String url = "https://query.wikidata.org/sparql?query=" + URLEncoder.encode(query, "UTF-8")
						+ "&format=json";
				// String res = IOUtils.toString(new URL(url), "UTF-8");

				URLConnection connection = new URL(url).openConnection();
				connection.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
				connection.connect();

				BufferedReader r = new BufferedReader(
						new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					sb.append(line);
				}
				String res = sb.toString();

				System.out.println("Store in " + fileName + ".");

				writer = FileLoader.getWriter(fileName);
				writer.write(res);

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}
		}

	}

	public void downloadDBPediaFiles() {

		DBPediaDumpFilesFinder ddff = new DBPediaDumpFilesFinder();
		ddff.init();

		for (Language language : this.languages) {
			Map<FileName, String> urls = ddff.getDBpediaURLsWithFileTypes(language, Config.getValue("dbpedia"));
			for (FileName fileType : urls.keySet()) {
				String url = urls.get(fileType);

				if (url == null) {
					System.out.println(
							"DBpedia: Skip " + fileType.getFileName() + " in " + language.getLanguageAdjective() + ".");
					continue;
				}

				String fileName = FileLoader.getPath(fileType, language);
				String fileNameBZ = fileName + ".bz2";

				File downloadedFile = downloadFile(url, fileNameBZ);

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
					continue;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (fin != null) {
							fin.close();
							out.close();
							bzIn.close();
							Files.delete(downloadedFile.toPath());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}

		// download ontology
		downloadFile(DBPediaDumpFilesFinder.getOntologyURL(),
				FileLoader.getFileNameWithPath(FileName.DBPEDIA_ONTOLOGY));
	}

	public void downloadYAGOFiles() {

		String yagoUrl = "http://resources.mpi-inf.mpg.de/yago-naga/yago3.1/";
		Map<String, FileName> urls = new HashMap<String, FileName>();
		urls.put("yagoTaxonomy.ttl.7z", FileName.YAGO_TAXONOMY);
		urls.put("yagoDateFacts.ttl.7z", FileName.YAGO_DATE_FACTS);
		urls.put("yagoFacts.ttl.7z", FileName.YAGO_FACTS);
		urls.put("yagoMetaFacts.ttl.7z", FileName.YAGO_META_FACTS);
		urls.put("yagoWikidataInstances.ttl.7z", FileName.YAGO_WIKIDATA_INSTANCES);
		urls.put("yagoLiteralFacts.ttl.7z", FileName.YAGO_LITERAL_FACTS);

		// try {
		// SevenZip.initSevenZipFromPlatformJAR();
		// } catch (SevenZipNativeInitializationException e) {
		// e.printStackTrace();
		// }

		for (String urlString : urls.keySet()) {
			File downloadedFile = null;

			// try {

			downloadedFile = downloadFile(yagoUrl + urlString, this.dataPath + "yago/" + urlString);

			downloadedFile = new File(this.dataPath + "yago/" + urlString);

			RandomAccessFile randomAccessFile = null;
			ISimpleInArchive inArchive = null;
			PrintWriter writer = null;

			try {
				writer = FileLoader.getWriter(urls.get(urlString));
				randomAccessFile = new RandomAccessFile(downloadedFile.getPath(), "r");
				inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile))
						.getSimpleInterface();

				// write the archive file content into new file
				for (ISimpleInArchiveItem item : inArchive.getArchiveItems()) {

					if (!item.isFolder()) {
						ExtractOperationResult result;

						final List<String> lineSets = new ArrayList<String>();

						result = item.extractSlow(new ISequentialOutStream() {
							public int write(byte[] data) throws SevenZipException {
								try {
									String str = new String(data, "UTF-8");
									lineSets.add(str);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								return data.length;
							}
						});

						for (String s : lineSets)
							writer.write(s);

						if (result != ExtractOperationResult.OK) {
							System.err.println("Error extracting item: " + result);
						}
					}
				}

			} catch (Exception e) {
				System.err.println("Error occurs: " + e);
			} finally {
				if (inArchive != null) {
					try {
						inArchive.close();
					} catch (SevenZipException e) {
						System.err.println("Error closing archive: " + e);
					}
				}
				if (randomAccessFile != null) {
					try {
						randomAccessFile.close();
					} catch (IOException e) {
						System.err.println("Error closing file: " + e);
					}
				}
				writer.close();
			}

			// Delete the archive file
			try {
				Files.delete(downloadedFile.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		downloadYAGODBPediaRelations();

	}

	private void downloadYAGODBPediaRelations() {

		File zippedFile = downloadFile("http://webdam.inria.fr/paris/yd_relations.zip",
				this.dataPath + "yago/" + "yd_relations.zip");

		ZipFile zipFile = null;

		try {
			zipFile = new ZipFile(zippedFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				System.out.println("\tIn Zip: " + entry.getName());

				FileName fileName = null;

				if (entry.getName().equals("relations_dbpedia_sub_yago.tsv"))
					fileName = FileName.YAGO_FROM_DBPEDIA_RELATIONS;
				else if (entry.getName().equals("relations_yago_sub_dbpedia.tsv"))
					fileName = FileName.YAGO_TO_DBPEDIA_RELATIONS;
				else {
					System.err.println("Unknown file: " + fileName);
					continue;
				}

				PrintWriter writer = null;
				InputStream stream = null;
				try {
					stream = zipFile.getInputStream(entry);

					writer = FileLoader.getWriter(fileName);
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

					while (reader.ready()) {
						String line = reader.readLine();
						writer.write(line + Config.NL);
					}

				} finally {
					stream.close();
					writer.close();
				}

			}
		} catch (ZipException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				zipFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Delete the archive file
		try {
			Files.delete(zippedFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
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

		int tries = 0;

		boolean succ = false;
		while (!succ) {
			tries += 1;
			System.out.println("Download file " + url + " to " + targetPath + ".");

			try {
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				fos = new FileOutputStream(targetPath);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				succ = true;
			} catch (FileNotFoundException e) {
				System.out.println(" Warning: File does not exist.");
				succ = true;
			} catch (IOException e) {
				if (e.getMessage().contains("response code: 503")) {

					if (tries == 5) {
						System.err.println("Could not download " + url + ". Continue.");
						return null;
					}

					// if server is overload: wait for 1 minute and re-try
					System.out.println(e.getMessage());
					try {
						Thread.sleep(60000);
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
