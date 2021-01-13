package de.l3s.eventkg.source.dbpedia.download;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.util.FileName;

public class DBPediaDumpFilesFinder {

	private String dbpediaQuery;

	public static void main(String[] args) {
		DBPediaDumpFilesFinder ddff = new DBPediaDumpFilesFinder();
		ddff.init();

		System.out.println(getOntologyURL());
		ddff.getDBpediaURLsWithFileTypes(Language.BG, "2020.10.01");
	}

	public Map<FileName, String> getDBpediaURLsWithFileTypes(Language language, String version) {

		Map<FileName, String> dbpediaURLs = new HashMap<FileName, String>();

		dbpediaURLs.put(FileName.DBPEDIA_GEO_COORDINATES,
				getURL(language, version, "https://databus.dbpedia.org/marvin/mappings/geo-coordinates-mappingbased"));
		dbpediaURLs.put(FileName.DBPEDIA_TYPES,
				getURL(language, version, "https://databus.dbpedia.org/marvin/mappings/instance-types"));
		// dbpediaURLs.put(FileName.DBPEDIA_TYPES_TRANSITIVE, getURL(language,
		// version,
		// "http://dbpedia-mappings.tib.eu/release/mappings/geo-coordinates-mappingbased/2019.09.01/dataid.ttl#Dataset"));
		dbpediaURLs.put(FileName.DBPEDIA_MAPPINGS,
				getURL(language, version, "https://databus.dbpedia.org/marvin/mappings/mappingbased-objects"));
		dbpediaURLs.put(FileName.DBPEDIA_MAPPINGS_LITERALS,
				getURL(language, version, "https://databus.dbpedia.org/marvin/mappings/mappingbased-literals"));
		// dbpediaURLs.put(FileName.DBPEDIA_GEONAMES_LINKS,
		// getURL(language, version,
		// "https://databus.dbpedia.org/kurzum/cleaned-data/geonames"));

		for (String v : dbpediaURLs.values())
			System.out.println(v);

		return dbpediaURLs;
	}

	public void init() {
		try {
			this.dbpediaQuery = IOUtils.toString(
					DBPediaDumpFilesFinder.class.getResourceAsStream("/resource/dbpedia/dbpedia_dump_file_urls.sparql"),
					"UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getURL(Language language, String version, String artifact) {

		String query = this.dbpediaQuery.replace("@version@", version);
		query = query.replace("@language@", language.getLanguageLowerCase());
		query = query.replace("@artifact@", artifact);

		try {
			query = URLEncoder.encode(query, "UTF-8");

			query = "https://databus.dbpedia.org/repo/sparql?default-graph-uri=&query=" + query
					+ "&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on";
			
			
			JSONObject json = new JSONObject(IOUtils.toString(new URL(query), Charset.forName("UTF-8")));
			JSONArray arr = json.getJSONObject("results").getJSONArray("bindings");
			return arr.getJSONObject(0).getJSONObject("url").getString("value");
		} catch (JSONException | IOException e) {
			System.out.println("Missing: " + artifact);
			return null;
			// e.printStackTrace();
		}

		// return null;
	}

	public static String getOntologyURL() {

		try {
			String query = IOUtils.toString(
					DBPediaDumpFilesFinder.class.getResourceAsStream("/resource/dbpedia/dbpedia_ontology_url.sparql"),
					"UTF-8");
			query = URLEncoder.encode(query, "UTF-8");

			query = "https://databus.dbpedia.org/repo/sparql?default-graph-uri=&query=" + query
					+ "&format=application%2Fsparql-results%2Bjson&timeout=0&debug=on";

			JSONObject json = new JSONObject(IOUtils.toString(new URL(query), Charset.forName("UTF-8")));
			JSONArray arr = json.getJSONObject("results").getJSONArray("bindings");
			return arr.getJSONObject(0).getJSONObject("file").getString("value");
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
