#!/bin/sh
for i in 1 4 8 16 24 32 40 48
do
echo "Running test for $i threads"
echo "TIME For $i threads" >> scaling_results.txt
java -XX:ActiveProcessorCount=$i -jar tap-eda.jar -d enedis.properties -t >> scaling_results.txt
done

echo "Done ... Filtering logs"
grep TIME < scaling_results.txt