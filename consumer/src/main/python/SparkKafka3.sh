# SparkKafka2.sh
#   --packages datastax:spark-cassandra-connector:2.4.0-s_2.11 ./SparkKafkaConsumer.py
export SPARK_HOME=/Users/jasonhaugland/datastax/spark-2.4.0-bin-hadoop2.7
$SPARK_HOME/bin/spark-submit \
--jars /Users/jasonhaugland/datastax/dse-6.7.1.jason1/clients/dse-byos_2.11-6.7.1.jar \
--properties-file /tmp/byos.properties \
--packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3  ./SparkKafkaConsumer.py
