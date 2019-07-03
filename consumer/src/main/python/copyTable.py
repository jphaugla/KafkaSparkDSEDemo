#standalone.py

from pyspark.sql import SparkSession

spark = SparkSession.builder \
        .master("local") \
        .appName("Copy sensor_meta") \
        .getOrCreate()

sensor_meta=spark.read.format("org.apache.spark.sql.cassandra").options(table="sensor_meta", keyspace="demo").load()
# sensor_meta.createCassandraTable("demo","dup_sensor_meta",partitionKeyColumns = Some(Seq(serial_number)))
#      this method, createCassandraTable, is not available in python
#      https://datastax-oss.atlassian.net/browse/SPARKC-525
#
save_options = { "table": "dup_sensor_meta", "keyspace": "demo", "confirm.truncate": "true"}
sensor_meta.write.format("org.apache.spark.sql.cassandra").options(**save_options).save(mode ="overwrite")
