package de.l3s.eventkg.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class URLUtil {

	public static void main(String[] args) {
		System.out.println(urlExists("http://bg.dbpedia.org/"));
	}

	// Source:
	// https://stackoverflow.com/questions/1378199/how-to-check-if-a-url-exists-or-returns-404-with-java

	public static boolean urlExists(String urlString) {

		URL url;
		try {
			url = new URL(urlString);

			// We want to check the current URL
			HttpURLConnection.setFollowRedirects(false);

			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

			// We don't need to get data
			httpURLConnection.setRequestMethod("HEAD");

			// Some websites don't like programmatic access so pretend to be a
			// browser
			httpURLConnection.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
			httpURLConnection.setConnectTimeout(3000);
			int responseCode = httpURLConnection.getResponseCode();

			// We only accept response code 200
			return responseCode == HttpURLConnection.HTTP_OK;

		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

}
