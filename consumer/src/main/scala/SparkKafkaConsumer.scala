package com.datastax.demo

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by carybourgeois on 10/30/15.
  *  Modified by jasonhaugland on 10/20/16.
  *  changed to structured streaming by jasonhaugland on 11/29/18.
 */

/**
  */
import java.sql.Timestamp
import java.text.{DateFormat, SimpleDateFormat}

import com.datastax.driver.core.Session

import collection.JavaConversions._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.streaming._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types.{IntegerType,StringType}

import org.apache.spark.sql.streaming.Trigger


object SparkKafkaConsumer {


  def main(args: Array[String]) {

    println(s"entered main")

    val sparkJob = new SparkJob()
    try {
      sparkJob.runJob()
    } catch {
      case ex: Exception =>
        println("error in main running spark job")
    }
  }
}

class SparkJob extends Serializable {

  println(s"before build spark session")

  def runJob() = {
  val appName = "SparkKafkaConsumer"

  val sparkSession =
    SparkSession.builder
      .appName(appName)
      .config("spark.cassandra.connection.host", "node0")
      .getOrCreate()

  println(s"after build spark session")
  val sensorMinuteFormat = new SimpleDateFormat("YYYYMMddHHmm")


  println(s"before reading kafka stream after runJob")

  import sparkSession.implicits._


    val sensDetailDS = sparkSession.readStream
      .format("kafka")
      .option("subscribe", "stream_ts")
      .option("failOnDataLoss", "false")
      .option("startingOffsets", "latest")
      .option("kafka.bootstrap.servers", "node0:9092")
      .option("includeTimestamp", true)
      .load()
      .selectExpr("CAST(value AS STRING)","CAST(timestamp as Timestamp)",
                  "CAST(key as STRING)")
      .as[(String, Timestamp, String)] 

    println(s"finished reading transaction kafka stream ")
    sensDetailDS.printSchema()
    val sens_df = sensDetailDS.withColumn("splitData", split(col("value"),";")).select(
					$"splitData".getItem(0).as("edge_id"),
					$"splitData".getItem(1).as("serial_number"),
					$"splitData".getItem(3).cast("Timestamp").as("ts"),
					$"splitData".getItem(4).cast("Double").as("depth"),
					$"splitData".getItem(5).cast("Double").as("value")
					)
    				.withColumn("time_bucket",date_format(col("ts"), "yyyyMMddHHmm")).as("time_bucket")
    sens_df.printSchema()

    val alarm_df = sens_df.select("edge_id","serial_number","ts","depth","value","time_bucket").where("value > 101.00" )

    println(s"alarm df ")

    alarm_df.printSchema()

    val windowedCount = sens_df
      .groupBy( window($"ts", "1 minutes", "1 minutes"),$"serial_number")
      .agg(
           max($"depth").alias("max_depth"),min($"depth").alias("min_depth"),
           mean($"depth").alias("mean_depth"),stddev($"depth").alias("stddev_depth"),
           avg($"depth").alias("avg_depth"), sum($"depth").alias("sum_depth"),
           max($"value").alias("max_value"),min($"value").alias("min_value"),
           mean($"value").alias("mean_value"),stddev($"value").alias("stddev_value"),
           avg($"value").alias("avg_value"), sum($"value").alias("sum_value"),
           count($"ts").alias("row_count")
          )
    println(s"after window ")

    val clean_df = windowedCount.selectExpr ( "serial_number",
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

    println(s"after clean_df ")
    clean_df.printSchema()

    val det_query = sens_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail/")
      .option("keyspace", "demo")
      .option("table", "sensor_detail")
      .outputMode(OutputMode.Update)
      .start()
    println (s"after write to sensor_detail")

    val dsefs_query = sens_df.writeStream
      .format("parquet")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/dsefs/")
      .option("path", "dsefs://node0:5598/parquet/")
      .trigger(Trigger.ProcessingTime("120 seconds"))
      .outputMode(OutputMode.Append)
      .start()
    println (s"after write to parquet")

    val det2_query = sens_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/detail2/")
      .option("keyspace", "demo")
      .option("table", "last")
      .outputMode(OutputMode.Update)
      .start()
    println (s"after write to last")

    val win_query = clean_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/summary/")
      .option("keyspace", "demo")
      .option("table", "sensor_summary")
      .outputMode(OutputMode.Update)
      .start()
    println (s"after write to sensor_summary")

    val alarm_query = alarm_df.writeStream
      .format("org.apache.spark.sql.cassandra")
      .option("checkpointLocation", "dsefs://node0:5598/checkpoint/alarm/")
      .option("keyspace", "demo")
      .option("table", "sensor_alarm")
      .outputMode(OutputMode.Update)
      .start()
    println (s"after write to sensor_alarm")

/*
    val sens_small_df = sens_df.select("serial_number","ts")

    val det_query = sens_small_df.writeStream
      .format("console")
      .option("truncate", "false")
      .outputMode(OutputMode.Update)
      .start()

    val clean_small_df = clean_df.select ("serial_number","time_bucket","row_count")

    val win_query = clean_small_df.writeStream
      .format("console")
      .option("truncate", "false")
      .outputMode(OutputMode.Update)
      .start()
*/

    win_query.awaitTermination()
    det_query.awaitTermination()
    det2_query.awaitTermination()
    dsefs_query.awaitTermination()
    alarm_query.awaitTermination()
//     better might be awaitAnyTermination
//    sparkSession.streams.awaitAnyTermination()
    println(s"after awaitTermination ")
    sparkSession.stop()
  }
}
