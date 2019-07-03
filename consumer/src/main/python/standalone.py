#standalone.py

from pyspark.sql import SparkSession

spark = SparkSession.builder \
        .master("local") \
        .appName("Read sensorsumm") \
        .getOrCreate()
spark.read.format("org.apache.spark.sql.cassandra").options(table="sensor_meta", keyspace="demo").load().show()
