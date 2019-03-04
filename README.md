# EventKG
[eventkg.l3s.uni-hannover.de](http://eventkg.l3s.uni-hannover.de/)

The EventKG is a novel multilingual resource incorporating event-centric information extracted from several large-scale knowledge graphs such as Wikidata, DBpedia and YAGO, as well as less structured sources such as Wikipedia Current Events and Wikipedia event lists in five languages. The EventKG is an extensible event-centric resource modeled in RDF. It relies on Open Data and best practices to make event data spread across different sources available through a common representation and reusable for a variety of novel algorithms and real-world applications.

## Configuration

Create a configuration file like the following to state where to store your EventKG version, and the languages and dumps to be used for extraction:

```
data_folder	/home/....../data
languages	en,de,ru,fr,pt,it
enwiki	20190101
dewiki	20190101
frwiki	20190101
ruwiki	20190101
ptwiki	20190101
dbpedia	2016-10
wikidata	20181231
```
Currently, the five languages English (en), German (de), Russian (ru), French (fr), and Portuguese (pt) are supported.
Timestamps of current Wikipedia dumps can be found on [https://dumps.wikimedia.org/enwiki](https://dumps.wikimedia.org/enwiki/). Usually, the dump dates are consistent between languages. The chosen dump needs to say "Dump complete" on the dump's website.
Wikidata dumps are listed on [https://dumps.wikimedia.org/wikidatawiki/entities/](https://dumps.wikimedia.org/wikidatawiki/entities/). There is one dump for each language.
DBpedia is dumped for all languages at once. The newest dump is listed on the top of [http://wiki.dbpedia.org/datasets](http://wiki.dbpedia.org/datasets).

## Run the extraction

The EventKG extraction pipeline consists of several steps described in the following. Consider that some of these step require some time and resources (e.g. for the data download, for processing the big Wikidata dump file, and for processing the Wikipedia XML files).

1. Export the Pipeline class (`de.l3s.eventkg.pipeline.Pipeline`) as executable jar (`Pipeline.jar`).

2. Start the data download using:

```java -jar Pipeline.jar path_to_config_file.txt 1```

3. Run the first steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 2,3```

4. Export the Dumper class (`de.l3s.eventkg.source.wikipedia.mwdumper.Dumper`) as Jar (`Dumper.jar`). Run the extraction from the Wikipedia dump files for each language by running the following command (here for Portuguese, replace `pt` with other languages if needed). [GNU parallel](https://www.gnu.org/software/parallel/) is required.

```nohup parallel -j9 "bzip2 -dc {} | java -jar -Xmx6G -Xss40m Dumper.jar path_to_config_file.txt pt" :::: data/raw_data/wikipedia/pt/dump_file_list.txt 2> log_dumper.txt```

5. Start the final steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 4,5,6,7,8```

6. The resulting *.nq* files can be found in the folder `data/output`.

## Manual Configuration

EventKG extracts information from several reference sources and fits them into the EventKG schema. Therefore, several expressions needs to be defined manually. This includes mappings from source-specific property labels to the EventKG schema and language-specific temporal expressions, as explained below. If the reference sources get updated or a new language is included in EventKG, manual changes are necessary for these files.

### Mappings

Several relations are directly mapped to properties defined in the EventKG schema, e.g. sem:hasSubEvent and sem:hasPlace. These mappings are defined in the following source-specific files.

| File name | Content Description | Example |
|--|--|--|
| [wikidata/property_names_locations.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikidata/property_names_locations.tsv) | Wikidata properties denoting location relations (e.g. FIFA World Cup 2018, sem:hasPlace, Russia). Each line contains a pair (property id \| property label). | P36 \| capital |
| [wikidata/temporal_property_names.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikidata/temporal_property_names.tsv) | Wikidata properties denoting time relations. Each line contains a triple (property id \| property label \| s/e/b), where "s" are start time, "e" end times and "b" both start and end times. | P569 \| date of birth \| s |
| [wikidata/properties_sublocations.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikidata/properties_sublocations.tsv) | Wikidata properties denoting sub and parent location relations (e.g. Paris, so:containedInPlace, France). Each line contains a triple (property id \| property label \| p/s), where "s" are sub relations and "p" are parent relations. | P1376 \| capital of \| s |
| [wikidata/event_blacklist_classes.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikidata/event_blacklist_classes.tsv) | Wikidata classes whose instances may not be identified as events. | Q1914636 \| activity |
| [yago/time_properties.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/yago/time_properties.tsv) |  YAGO properties denoting time relations. Each line contains a pair (property id \| s/e/b), where "s" are start time, "e" end times and "b" both start and end times. | \<happenedOnDate\> \| b |
| [dbpedia/part_of_properties.tsv](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/dbpedia/part_of_properties.tsv) |  DBpedia properties denoting "part of" relations. | \<isPartOf\> |

### Terms

For each language, a list of terms is needed that is used when extracting data from Wikipedia.

Examples:
 - [English file](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikipedia/en/event_date_expressions.txt)
 - [German file](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikipedia/de/event_date_expressions.txt)

| Name | Meaning | Examples (en) |
|--|--|--|
| forbiddenLinks | Links that are ignored | Wikipedia:Citation_needed|
| forbiddenNameSpaces | Link namespaces that are ignored | Talk, User|
| talkSuffix | Suffix of talk/discussion pages | talk |
| talkPrefix | Prefix of talk/discussion pages |  |
| forbiddenInternalLinks |  | WT:, H: |
| tableOfContents | Section title of the table of contents in the Wikipedia pages | Contents |
| seeAlsoLinkClasses | CSS class of "see also" links in Wikipedia | hatnote |
| titlesNotToTranslate | Section titles in the Wikipedia pages that are ignored | See also, References |
| fileLabels | Prefix of file links | File |
| categoryLabel | Prefix of category pages | Category |
| imageLabels | Prefix of image links | Image |
| listPrefixes | Prefix of Wikipedia pages that are lists | Lists_of_ |
| categoryPrefixes | Prefix of category pages | Category: |
| eventsLabels | Section titles in Wikipedia event pages that denote list of textual events  | Events |
| monthNames | List of month names (starting from January, one weekday per line. Alternatives separated by ";") | January |
| weekdayNames | List of weekday names (starting from Monday, one weekday per line. Alternatives separated by ";") | Monday |
| eventCategoryRegexes | Regexes that match Wikipedia categories with event pages (e.g. "Category:Political_events") | .+events$ |

### Event Date Expressions

For each language, a list of time expressions is needed that is used when extracting textual events from Wikipedia event lists.

Examples:
 - [English file](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikipedia/en/event_date_expressions.txt)
 - [German file](https://github.com/sgottsch/eventkg/blob/master/src/resource/meta_data/wikipedia/de/event_date_expressions.txt)

| Name | Meaning |
|--|--|
| predefined regexes | A set of placeholders which are given in the code and can be re-used. No need to change this |
| new regexes | New placeholders that can be re-used later on. |
| dayTitle | Regex for Wikipedia page titles that represent a specific day. For example "^@regexMonth1@ @regexDay1@$" to find "March 15" for the Wikipedia article https://en.wikipedia.org/wiki/March_15. Other example: en: January 22, de: 22. Januar, fr: 22 janvier, pt: 22 de janeiro, ru: 22 января |
| yearTitlePatterns | Regexes for Wikipedia page titles that represent a specific year. For example "^(?<y>[0-9]{@digitsInYear@}) in .*$" to find "2007 in philosophy" for the Wikipedia article https://en.wikipedia.org/wiki/2007_in_philosophy. |
| datePatterns | A list of regexes to extract date expressions from event texts. |
| dateLinkResolvers | Sometimes, dates are given as links, which are resolved using these regexes. The "<r>" group denotes the anchor text. For example to find 474 BC in "*[[474 BC]] &ndash; [[Roman consul]] ...". |

## License

This project is licensed under the terms of the MIT license (see LICENSE.txt).