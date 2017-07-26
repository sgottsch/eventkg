# EventKG
[eventkg.l3s.uni-hannover.de](http://eventkg.l3s.uni-hannover.de/)

The EventKG is a novel multilingual resource incorporating event-centric information extracted from several large-scale knowledge graphs such as Wikidata, DBpedia and YAGO, as well as less structured sources such as Wikipedia Current Events and Wikipedia event lists in five languages. The EventKG is an extensible event-centric resource modeled in RDF. It relies on Open Data and best practices to make event data spread across different sources available through a common representation and reusable for a variety of novel algorithms and real-world applications.

#### Configuration

Create a configuration file like the following to state where to store your EventKG version, and the languages and dumps to be used for extraction:

```
data_folder	/home/....../data
languages	en,de,ru,fr,pt
enwiki	20170420
dewiki	20170420
frwiki	20170420
ruwiki	20170420
ptwiki	20170420
dbpedia	2016-10
wikidata	20170424
```
Currently, the five languages English (en), German (de), Russian (ru), French (fr), and Portuguese (pt) are supported.
Timestamps of current Wikipedia dumps can be found on [https://dumps.wikimedia.org/enwiki](https://dumps.wikimedia.org/enwiki/). Usually, the dump dates are consistent between languages. The chosen dump needs to say "Dump complete" on the dump's website.
Wikidata dumps are listed on [https://dumps.wikimedia.org/wikidatawiki/entities/](https://dumps.wikimedia.org/wikidatawiki/entities/). There is one dump for each language.
DBpedia is dumped for all languages at once. The newest dump is listed on the top of [http://wiki.dbpedia.org/datasets](http://wiki.dbpedia.org/datasets).

#### Run the extraction

The EventKG extraction pipeline consists of several steps described in the following. Consider that some of these step require some time and resources (e.g. for the data download, for processing the big Wikidata dump file, and for processing the Wikipedia XML files).

1. Export the Pipeline class (`de.l3s.eventkg.pipeline.Pipeline`) as executable jar (`Pipeline.jar`).

2. Start the data download using:

```java -jar Pipeline.jar path_to_config_file.txt 1```

3. Run the first steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 2,3```

4. Export the Dumper class (`de.l3s.eventkg.wikipedia.mwdumper.Dumper`) as Jar (`Dumper.jar`). Run the extraction from the Wikipedia dump files for each language by running the following command (here for Portuguese, replace `pt` with other languages if needed). [GNU parallel](https://www.gnu.org/software/parallel/) is required.

```nohup parallel -j9 "bzip2 -dc {} | java -jar -Xmx6G -Xss40m Dumper.jar path_to_config_file.txt pt" :::: data/raw_data/wikipedia/pt/dump_file_list.txt 2> log_dumper.txt```

5. Start the final steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 4,5```

6. The resulting *.nq* files can be found in the folder `data/results/all`.