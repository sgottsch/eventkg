nohup java -jar jars/Pipeline.jar config_eventkg.txt 1 > logs/nohup_1.out
nohup java -jar -Xmx70G jars/Pipeline.jar config_eventkg.txt 2,3 > logs/nohup_2_3.out

languages="da hr nl es bg no sl ro pl de ru fr pt it en"
for language in $languages; do
   nohup parallel -j9 "bzip2 -dc {} | java -jar -Xmx6G -Xss40m jars/Dumper.jar config_eventkg.txt ${language}" :::: data/raw_data/wikipedia/${language}/dump_file_list.txt 2> logs/log_dumper_${language}.txt > logs/nohup_${language}.out
done

nohup java -jar -Xmx70G jars/Pipeline.jar config_eventkg.txt 4 > logs/nohup_4.out
nohup java -jar -Xmx80G jars/Pipeline.jar config_eventkg.txt 5 > logs/nohup_5.out
nohup java -jar -Xmx80G jars/Pipeline.jar config_eventkg.txt 6 > logs/nohup_6.out
nohup java -jar -Xmx80G jars/Pipeline.jar config_eventkg.txt 7 > logs/nohup_7.out
nohup java -jar -Xmx100G jars/Pipeline.jar config_eventkg.txt 8 > logs/nohup_8.out
nohup java -jar -Xmx100G jars/Pipeline.jar config_eventkg.txt 9 > logs/nohup_9.out
nohup java -jar -Xmx100G jars/Pipeline.jar config_eventkg.txt 10 > logs/nohup_10.out