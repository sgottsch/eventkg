package de.l3s.eventkg.extension;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.meta.Language;

public class NamespaceNamesLoader {

	public static void main(String[] args) {
		System.out.println(getNamespaces(Language.DE));
		System.out.println(getMonthNames(Language.NL));
		System.out.println(getWeekdayNames(Language.NL));
	}

	public static List<String> getNamespaces(Language language) {

		List<String> namespaces = new ArrayList<String>();

		String url = "https://" + language.getLanguageLowerCase()
				+ ".wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=namespaces&format=json";

		try {
			JSONObject json = new JSONObject(IOUtils.toString(new URL(url), "UTF-8"));
			json = json.getJSONObject("query").getJSONObject("namespaces");
			for (int i = 0; i < json.names().length(); i++) {
				JSONObject jsonNamespace = json.getJSONObject(json.names().getString(i));
				namespaces.add(jsonNamespace.getString("*"));
			}
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}

		return namespaces;
	}

	public static List<String> getMonthNames(Language language) {
		DateFormatSymbols dfs = new DateFormatSymbols(language.getLocale());
		List<String> monthNames = new ArrayList<String>();
		for (String month : dfs.getMonths()) {
			if (month.isEmpty())
				continue;
			String line = month;
			if (Character.isLowerCase(month.charAt(0))) {
				line += ";" + StringUtils.capitalize(month);
			}
			monthNames.add(line);
		}
		return monthNames;
	}

	public static List<String> getWeekdayNames(Language language) {
		DateFormatSymbols dfs = new DateFormatSymbols(language.getLocale());
		List<String> weekdayNames = new ArrayList<String>();
		for (String weekday : dfs.getWeekdays()) {
			if (weekday.isEmpty())
				continue;
			String line = weekday;
			if (Character.isLowerCase(weekday.charAt(0))) {
				line += ";" + StringUtils.capitalize(weekday);
			}
			weekdayNames.add(line);
		}
		return weekdayNames;
	}

}
