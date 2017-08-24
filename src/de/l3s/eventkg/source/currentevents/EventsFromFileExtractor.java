package de.l3s.eventkg.source.currentevents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.currentevents.data.DataStore;
import de.l3s.eventkg.source.currentevents.model.Category;
import de.l3s.eventkg.source.currentevents.model.Source;
import de.l3s.eventkg.source.currentevents.model.Story;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class EventsFromFileExtractor extends Extractor {

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	private DataStore dataStore;

	public EventsFromFileExtractor(List<Language> languages) {
		super("EventsFromFileExtractor", de.l3s.eventkg.meta.Source.WCE, "Extract event pages.", languages);
	}

	public void run() {
		loadFiles();
		printEvents();
	}

	private void printEvents() {

		PrintWriter resultWriter = null;
		try {
			resultWriter = FileLoader.getWriter(FileName.WCE_EVENTS);

			Set<String> storyNames = new HashSet<String>();
			for (WCEEvent event : dataStore.getEvents()) {
				if (event.getStory() != null) {
					String storyName = event.getStory().getUrl()
							.substring(event.getStory().getUrl().lastIndexOf("/") + 1);
					storyNames.add(storyName);
				}
				// System.out.println(event.getDate());
			}

			for (String storyName : storyNames) {
				resultWriter.write(storyName + "\n");
				// System.out.println(storyName);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			resultWriter.close();
		}

	}

	public void loadFiles() {

		this.dataStore = new DataStore();

		for (File child : FileLoader.getFilesList(FileName.WCE_EVENTS_FOLDER)) {
			processFile(child);
		}

	}

	// public void loadFile(String fileName) {
	//
	// this.dataStore = new DataStore();
	//
	// URL url =
	// EventsFromFileExtractor.class.getResource("/resource/currentevents/" +
	// fileName);
	//
	// File file;
	// try {
	// file = new File(url.toURI());
	// processFile(file);
	// } catch (URISyntaxException e) {
	// e.printStackTrace();
	// }
	//
	// }

	private void processFile(File file) {

		try {
			String content = FileLoader.readFile(file);
			JSONArray arr = new JSONArray(content);

			processJSONArray(arr);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void processJSONArray(JSONArray arr) {

		for (int i = 0; i < arr.length(); i++) {
			JSONObject obj = arr.getJSONObject(i);
			parseJSONObject(obj);
		}

	}

	private void parseJSONObject(JSONObject obj) {

		Date date = null;

		try {
			date = format.parse(obj.getString("date"));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String description = obj.getString("description");

		long id = obj.getLong("id");

		Category category = null;
		if (obj.has("category"))
			category = parseCategoryObject(obj.getJSONObject("category"));

		Story story = null;
		if (obj.has("story")) {
			story = parseStoryObject(obj.getJSONObject("story"));
		}
		if (obj.has("belongsToStory")) {
			story = parseStoryObject(obj.getJSONObject("belongsToStory"));
		}

		Set<WCEEntity> entities = parseEntityArray(obj.getJSONArray("entity"));
		Set<Source> sources = parseSourceArray(obj.getJSONArray("source"));

		WCEEvent event = new WCEEvent(id, date, description, category, story, entities, sources);
		event.setMethod("ev");
		dataStore.getEventsByID().put(event.getId(), event);
	}

	private Set<WCEEntity> parseEntityArray(JSONArray arr) {

		Set<WCEEntity> entities = new HashSet<WCEEntity>();

		for (int i = 0; i < arr.length(); i++) {
			entities.add(parseEntityObject(arr.getJSONObject(i)));
		}

		return entities;
	}

	private WCEEntity parseEntityObject(JSONObject obj) {

		String name = obj.getString("name");
		String url = obj.getString("wikiURL");
		long id = obj.getLong("id");

		WCEEntity entity = dataStore.getEntity(id, name, url);

		return entity;
	}

	private Category parseCategoryObject(JSONObject obj) {

		String name = obj.getString("name");
		long id = obj.getLong("id");

		Category category = dataStore.getCategory(id, name);

		return category;
	}

	private Story parseStoryObject(JSONObject obj) {

		String name = obj.getString("name");
		long id = obj.getLong("id");
		String url = obj.getString("wikipediaUrl");

		Story story = dataStore.getStory(id, name, url);

		return story;
	}

	private Set<Source> parseSourceArray(JSONArray arr) {

		Set<Source> sources = new HashSet<Source>();

		for (int i = 0; i < arr.length(); i++) {
			sources.add(parseSourceObject(arr.getJSONObject(i)));
		}

		return sources;
	}

	private Source parseSourceObject(JSONObject obj) {

		String url = obj.getString("url");
		long id = obj.getLong("id");
		String type = obj.getString("type");
		String sourceName = obj.getString("sourceName");

		Source source = dataStore.getSource(id, url, type, sourceName);

		return source;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

}
