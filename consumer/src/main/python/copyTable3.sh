#  this is for BYOS
# copyTable3.sh
export SPARK_HOME=/Users/jasonhaugland/datastax/spark-2.4.0-bin-hadoop2.7
$SPARK_HOME/bin/spark-submit \
--jars /Users/jasonhaugland/datastax/dse-6.7.1.jason1/clients/dse-byos_2.11-6.7.1.jar \
--properties-file /tmp/byos.properties ./copyTable.py

