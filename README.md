# EventKG
[http://eventkg.l3s.uni-hannover.de/](http://eventkg.l3s.uni-hannover.de/)

MultiWiki is a graphical Web user interface that presents semantic-based text passage extraction and alignment across the interlingual article pairs in Wikipedia. This approach enables identification and interlinking of text passages written in different languages and containing overlapping information with respect to a common topic. Semantic-based text passage alignment can enable Wikipedia editors and readers to better understand language-specific context of entities and events, provide valuable insides in cultural differences, and build a basis for qualitative analysis of Wikipedia articles.

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
```java -jar Pipeline.jar path_to_config_file.txt 2,3```

Specify the language in the Dumper class and export it as Jar. Run the extraction from the Wikipedia dump files for each language:

```nohup parallel -j9 "bzip2 -dc {} | java -jar -Xmx6G -Xss40m Dumper.jar --format=eventkb:-1" :::: data/raw_data/wikipedia/fr/dump_file_list.txt 2> log_dumper.txt```

Start the final steps of extraction:

```java -jar Pipeline.jar path_to_config_file.txt 4,5```
