# EventKG
[http://eventkg.l3s.uni-hannover.de/](http://eventkg.l3s.uni-hannover.de/)

The EventKG is a novel multilingual resource incorporating event-centric information extracted from several large-scale knowledge graphs such as Wikidata, DBpedia and YAGO, as well as less structured sources such as Wikipedia Current Events and Wikipedia event lists in five languages. The EventKG is an extensible event-centric resource modeled in RDF. It relies on Open Data and best practices to make event data spread across different sources available through a common representation and reusable for a variety of novel algorithms and real-world applications.

#### Configuration

Create a configuration file like the following to state where to store your EventKG version, from which languages and dumps to extract.:

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
wikidata_meta_files	wikidata_meta_files
```

#### Run the extraction

Export the Pipeline class as exectuable jar and start the data download using:

```java -jar Pipeline.jar path_to_config_file.txt 1```

Run the first steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 2,3```

Specify the language in the Dumper class and export it as Jar. Run the extraction from the Wikipedia dump files for each language:

```nohup parallel -j9 "bzip2 -dc {} | java -jar -Xmx6G -Xss40m Dumper.jar --format=eventkb:-1" :::: data/raw_data/wikipedia/fr/dump_file_list.txt 2> log_dumper.txt```

Start the final steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 4,5```
