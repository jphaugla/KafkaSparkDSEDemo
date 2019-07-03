#    Kafka
# from pyspark.streaming.kafka import KafkaUtils

from pyspark.sql import SparkSession
from pyspark.sql.functions import split, col, date_format, window, max, min, mean, avg, stddev, sum, count
# from pyspark.sql.streaming import Trigger

sparkSession = SparkSession.builder \
    .master("local") \
    .appName("SparkKafkaConsumer") \
    .getOrCreate()

print("after build spark session")

print ("before reading kafka stream after runJob")

sensDetailDS = sparkSession.readStream \
  .format("kafka") \
  .option("subscribe", "stream_ts") \
  .option("failOnDataLoss", "false") \
  .option("startingOffsets", "latest") \
  .option("kafka.bootstrap.servers", "node0:9092") \
  .option("includeTimestamp", "true") \
  .load() \
  .selectExpr("CAST(value AS STRING)","CAST(timestamp as Timestamp)", "CAST(key as STRING)")
# .as[(String, Timestamp, String)]

print("finished reading transaction kafka stream ")
sensDetailDS.printSchema()
sens_df = sensDetailDS.withColumn("splitData", split(col("value"),";")) \
		      .select(
 			col("splitData").getItem(0).alias("edge_id"),
                        col("splitData").getItem(1).alias("serial_number"),
                        col("splitData").getItem(3).cast("Timestamp").alias("ts"),
                        col("splitData").getItem(4).cast("Double").alias("depth"),
                        col("splitData").getItem(5).cast("Double").alias("value")
                       ) \
                      .withColumn("time_bucket",date_format(col("ts"), "yyyyMMddHHmm")).alias("time_bucket")
sens_df.printSchema()

alarm_df = sens_df.select("edge_id","serial_number","ts","depth","value","time_bucket").where("value > 101.00" )

print("alarm df ")

alarm_df.printSchema()

windowedCount = sens_df \
  .groupBy( window("ts", "1 minutes", "1 minutes"),"serial_number") \
  .agg(
       max("depth").alias("max_depth"),min("depth").alias("min_depth"),
       mean("depth").alias("mean_depth"),stddev("depth").alias("stddev_depth"),
       avg("depth").alias("avg_depth"), sum("depth").alias("sum_depth"),
       max("value").alias("max_value"),min("value").alias("min_value"),
       mean("value").alias("mean_value"),stddev("value").alias("stddev_value"),
       avg("value").alias("avg_value"), sum("value").alias("sum_value"),
       count("ts").alias("row_count")
      ) 
print("after window ")

clean_df = windowedCount.selectExpr ( "serial_number",
    "Cast(date_format(window.start, 'yyyyMMddHHmm') as string) as time_bucket",
    "Cast(max_depth as double) as max_depth",
    "Cast(min_depth as double) as min_depth",
    "Cast(avg_depth as double) as avg_depth",
    "Cast(sum_depth as double) as sum_depth",
    "Cast(mean_depth as double) as mean_depth",
    "Cast(stddev_depth as double) as stddev_depth",
    "Cast(max_value as double) as max_value",
    "Cast(min_value as double) as min_value",
    "Cast(avg_value as double) as avg_value",
    "Cast(sum_value as double) as sum_value",
    "Cast(mean_value as double) as mean_value",
    "Cast(stddev_value as double) as stddev_value",
    "Cast(row_count as int) as row_count"
    )

print("after clean_df ")
clean_df.printSchema()

det_query = sens_df.writeStream \
  .format("org.apache.spark.sql.cassandra") \
  .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail/") \
  .option("keyspace", "demo") \
  .option("table", "sensor_detail") \
  .outputMode("update") \
  .start()
print ("after write to sensor_detail")

#   this writes to dsefs file system
#   to increase size of the parquet files, increase the processing time window
dsefs_query = sens_df.writeStream \
  .format("parquet") \
  .option("checkpointLocation", "dsefs://node0:5598/checkpoint/dsefs/") \
  .option("path", "dsefs://node0:5598/parquet/") \
  .trigger(Trigger.ProcessingTime("240 seconds")) \
  .outputMode("append") \
  .start()
print ("after write to parquet")

det2_query = sens_df.writeStream \
  .format("org.apache.spark.sql.cassandra") \
  .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail2/") \
  .option("keyspace", "demo") \
  .option("table", "last") \
  .outputMode("update") \
  .start()
print ("after write to last")

win_query = clean_df.writeStream \
  .format("org.apache.spark.sql.cassandra") \
  .option("checkpointLocation", "dsefs://node0:5598/checkpoint/summary/") \
  .option("keyspace", "demo") \
  .option("table", "sensor_summary") \
  .outputMode("update") \
  .start()
print ("after write to sensor_summary")

alarm_query = alarm_df.writeStream \
  .format("org.apache.spark.sql.cassandra") \
  .option("checkpointLocation", "dsefs://node0:5598/checkpoint/alarm/") \
  .option("keyspace", "demo") \
  .option("table", "sensor_alarm") \
  .outputMode("update") \
  .start()
print ("after write to sensor_alarm")

win_query.awaitTermination()
det_query.awaitTermination()
det2_query.awaitTermination()
dsefs_query.awaitTermination()
alarm_query.awaitTermination()
#    better might be awaitAnyTermination
#    sparkSession.streams.awaitAnyTermination()
print("after awaitTermination ")
sparkSession.stop()
