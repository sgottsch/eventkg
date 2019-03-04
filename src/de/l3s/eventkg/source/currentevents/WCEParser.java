package de.l3s.eventkg.source.currentevents;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.eventkg.source.currentevents.model.Category;
import de.l3s.eventkg.source.currentevents.model.Source;
import de.l3s.eventkg.source.currentevents.model.Story;
import de.l3s.eventkg.source.currentevents.model.WCEEntity;
import de.l3s.eventkg.source.currentevents.model.WCEEvent;

public class WCEParser {

	private long eventId = 0l;
	private long storyId = 0l;
	private long categoryId = 0l;
	private long entityId = 0l;
	private long sourceId = 0l;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MMM-dd", Locale.US);

	private boolean print = false;

	public static void main(String[] args) throws JSONException, MalformedURLException, IOException {

		WCEParser p = new WCEParser();
		// p.getEvents("January_2019");
		// p.getEvents("December_2017");
		// p.getEvents("June_2017");
		// p.getEvents("August_2002");
		// p.getEvents("March_2001");

		p.getEvents("May_2008");

	}

	public void getEvents(String month) throws JSONException, MalformedURLException, IOException {

		String url = "https://en.wikipedia.org/w/api.php?action=parse&page=Portal:Current_events/" + month
				+ "&action=parse&format=json";
		JSONObject json = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));

		getEvents(json);
	}

	public List<WCEEvent> getEvents(JSONObject json) throws JSONException, MalformedURLException, IOException {

		String content = json.getJSONObject("parse").getJSONObject("text").getString("*");

		Document doc = Jsoup.parse(content);

		Map<String, Category> categories = new HashMap<String, Category>();
		Map<String, WCEEntity> entities = new HashMap<String, WCEEntity>();

		List<WCEEvent> events = new ArrayList<WCEEvent>();

		// There are basically three cases:
		// a) divs and categories
		// b) tables and categories
		// c) tables and no categories
		// We try a first, and then b + c together

		for (Element dayDiv : doc.select("div[class=\"vevent\"]")) {

			String dateString = dayDiv.select("span[class*=\"bday\"]").get(0).text();

			Date date = parseDate(dateString);

			if (print)
				System.out.println(date);

			for (Element descriptionDiv : dayDiv.select("div[class=\"description\"]")) {

				for (Element headingDiv : descriptionDiv.select("div[role=\"heading\"]")) {
					if (print)
						System.out.println("\t" + headingDiv.text());

					String categoryText = headingDiv.text();
					Category category = categories.get(categoryText);
					if (category == null) {
						category = new Category(this.categoryId, categoryText);
						categories.put(categoryText, category);
						this.categoryId += 1;
					}

					Element el = headingDiv.nextElementSibling();

					Story story = null;
					Element storyList = null;
					for (Element li : el.getElementsByTag("li")) {

						if (!li.getElementsByTag("ul").isEmpty()) {
							// List element is a story
							String text = li.getElementsByTag("a").get(0).text();
							if (print)
								System.out.println("\t\tS: " + text);
							String wikiLink = createWikiLink(li.getElementsByTag("a").get(0));
							story = new Story(this.storyId, text, wikiLink);
							storyList = li;
							this.storyId += 1;
						} else {
							// List element is an event

							if (storyList != null && !storyList.getAllElements().contains(li)) {
								// An event can have no story. Then it's not
								// nested within a story <li>.
								story = null;
								storyList = null;
							}

							String text = li.text();

							// extract sources

							Set<WCEEntity> eventEntities = new HashSet<WCEEntity>();
							Set<Source> eventSources = new HashSet<Source>();
							// extract entities
							for (Element a : li.select("a")) {
								if (a.classNames().contains("external")) {
									String link = a.attr("href");
									String sourceName = a.text();
									// remove bracketes
									if (sourceName.length() <= 2)
										continue;
									sourceName = sourceName.substring(1, sourceName.length() - 1);

									if (print)
										System.out.println("Source: " + link + " - " + sourceName);

									Source source = new Source(this.sourceId, link, "article", sourceName);
									eventSources.add(source);
									this.sourceId += 1;
								} else {
									String link = createWikiLink(a);
									// String name = a.text();
									WCEEntity entity = entities.get(link);
									if (entity == null) {
										entity = new WCEEntity(this.entityId, link);
										entities.put(link, entity);
										this.entityId += 1;
									}
									eventEntities.add(entity);
								}
							}

							WCEEvent event = new WCEEvent(this.eventId, date, text, category, story, eventEntities,
									eventSources);
							this.eventId += 1;
							events.add(event);

							if (print)
								System.out.println("\t\t\tE: " + li.text());
						}
					}

				}
			}

		}

		for (Element dayDiv : doc.select("table[class=\"vevent\"]")) {

			String dateString = dayDiv.select("span[class*=\"bday\"]").get(0).text();

			Date date = parseDate(dateString);

			if (print)
				System.out.println(dateString);

			for (Element descriptionDiv : dayDiv.select("td[class=\"description\"]")) {

				boolean hasCategory = true;

				Elements categoryElements = descriptionDiv.select("dl");
				if (categoryElements.isEmpty()) {
					categoryElements.add(dayDiv);
					hasCategory = false;
				}

				for (Element headingDiv : categoryElements) {

					Category category = null;
					if (hasCategory) {

						String categoryText = headingDiv.text();

						if (print)
							System.out.println("\t" + categoryText);

						category = categories.get(categoryText);
						if (category == null) {
							category = new Category(this.categoryId, categoryText);
							categories.put(categoryText, category);
							this.categoryId += 1;
						}
					}

					Element el = headingDiv.nextElementSibling();
					if (!hasCategory)
						el = headingDiv.select("ul").first();

					Story story = null;
					Element storyList = null;
					if (el != null) {
						for (Element li : el.getElementsByTag("li")) {

							if (!li.getElementsByTag("ul").isEmpty()) {
								// List element is a story
								String text = li.getElementsByTag("a").get(0).text();
								if (print)
									System.out.println("\t\tS: " + text);
								String wikiLink = createWikiLink(li.getElementsByTag("a").get(0));
								story = new Story(this.storyId, text, wikiLink);
								storyList = li;
								this.storyId += 1;
							} else {
								// List element is an event

								if (storyList != null && !storyList.getAllElements().contains(li)) {
									// An event can have no story. Then it's not
									// nested within a story <li>.
									story = null;
									storyList = null;
								}

								String text = li.text();

								// extract sources

								Set<WCEEntity> eventEntities = new HashSet<WCEEntity>();
								Set<Source> eventSources = new HashSet<Source>();
								// extract entities
								for (Element a : li.select("a")) {
									if (a.classNames().contains("external")) {
										String link = a.attr("href");
										String sourceName = a.text();
										// remove bracketes
										if (sourceName.length() <= 2)
											continue;
										sourceName = sourceName.substring(1, sourceName.length() - 1);

										if (print)
											System.out.println("Source: " + link + " - " + sourceName);

										Source source = new Source(this.sourceId, link, "article", sourceName);
										eventSources.add(source);
										this.sourceId += 1;
									} else {
										String link = createWikiLink(a);
										// String name = a.text();
										WCEEntity entity = entities.get(link);
										if (entity == null) {
											entity = new WCEEntity(this.entityId, link);
											entities.put(link, entity);
											this.entityId += 1;
										}
										eventEntities.add(entity);
									}
								}

								WCEEvent event = new WCEEvent(this.eventId, date, text, category, story, eventEntities,
										eventSources);
								this.eventId += 1;
								events.add(event);

								if (print)
									System.out.println("\t\t\tE: " + li.text());
							}
						}
					}
				}
			}

		}

		if (print) {
			for (WCEEvent event : events) {
				String storyString = ", Story: ";
				String categoryString = ", Category: ";
				if (event.getCategory() != null)
					categoryString += event.getCategory().getName();
				if (event.getStory() != null)
					storyString += event.getStory().getName();
				System.out.println(event.getDate() + categoryString + storyString);
				System.out.println("\t" + event.getDescription());
				for (WCEEntity entity : event.getEntities())
					System.out.println("\t\t" + entity.getWikiURL());
				for (Source source : event.getSources())
					System.out.println("\t\tSource: " + source.getSourceName() + ", " + source.getUrl());
			}
		}

		// Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		// String eventsJSON = gson.toJson(events);

		return events;
	}

	private Date parseDate(String dateString) {
		Date date = null;

		if (dateString.length() <= 10)
			try {
				date = dateFormat.parse(dateString);
			} catch (ParseException e) {
				e.printStackTrace();
			}

		else {
			try {
				date = dateFormat2.parse(dateString);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return date;
	}

	private String createWikiLink(Element a) {
		String wikiLink = a.attr("href");
		wikiLink = wikiLink.replace("/wiki/", "");
		try {
			wikiLink = URLDecoder.decode(wikiLink, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return wikiLink;
	}

}
