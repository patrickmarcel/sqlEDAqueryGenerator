#!/bin/sh
for i in 10 20 30 40 50 60 70 80 90
do

#WSC-unb-approx
java -XX:ActiveProcessorCount=16 -jar tap-eda.jar  -d enedis.properties -t -s $i -q 10 >> rd_samp.txt


#WSC-rand-approx
java -XX:ActiveProcessorCount=16 -jar tap-eda.jar  -d enedis.properties -t -s $i -u -q 10 >> rd_samp_uni.txt


done