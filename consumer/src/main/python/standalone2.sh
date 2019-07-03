# standalone2.sh
export SPARK_HOME=/Users/jasonhaugland/datastax/spark-2.4.0-bin-hadoop2.7
$SPARK_HOME/bin/spark-submit \
  --conf spark.cassandra.connection.host=127.0.0.1 \
  --packages datastax:spark-cassandra-connector:2.4.0-s_2.11  ./standalone.py
