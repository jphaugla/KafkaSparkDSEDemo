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
  *  Modifiied by jaosnhaugland on 10/20/16.
 */

import java.sql.Timestamp
import java.text.SimpleDateFormat

import com.datastax.spark.connector._
import com.datastax.spark.connector.writer.{TTLOption, WriteConf}
import kafka.serializer.StringDecoder
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Milliseconds, Seconds, StreamingContext, Time}
import org.apache.spark.{SparkConf, SparkContext}

case class SensorEvent(edgeId: String, sensorId: String, ts10min: String, ts: Timestamp, depth: Double, value: Double)
//  ValStream and SumTableStream used to write to summary table
case class ValStream(max_depth: Float, max_value:Float, ts10min:String,
                     min_depth: Float, min_value: Float, sum_depth: Float,
                     sum_value: Float, row_count:Integer)
case class SumTableStream(serial_number:String,max_depth: Float, max_value : Float, ts10min: String,
                          min_depth: Float, min_value: Float, sum_depth: Float,
                          sum_value: Float, row_count:Integer)
// This implementation uses the Kafka Direct API supported in Spark 1.4
object SparkKafkaConsumer extends App {

  val appName = "SparkKafkaConsumer"

  val conf = new SparkConf()
    .set("spark.cores.max", "2")
    .set("spark.executor.memory", "512M")
    .setAppName(appName)
  val sc = SparkContext.getOrCreate(conf)

  val sqlContext = SQLContext.getOrCreate(sc)
  import sqlContext.implicits._


  val ssc = new StreamingContext(sc, Milliseconds(10000))
  ssc.checkpoint(appName)

  val kafkaTopics = Set("stream_ts")
  val kafkaParams = Map[String, String]("metadata.broker.list" -> "localhost:9092")

  val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, kafkaTopics)

  kafkaStream
    .foreachRDD {
      (message: RDD[(String, String)], batchTime: Time) => {
        val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val ts = Timestamp.valueOf(payload(3))
          val sensorMinuteFormat = new SimpleDateFormat("YYYYMMddHHmm")
          //   since want every 10 minutes, drop off the last minute place
          val currentMinute = sensorMinuteFormat.format(ts).dropRight(1)
          SensorEvent(payload(0), payload(1), currentMinute, ts, payload(4).toDouble, payload(5).toDouble)
        }).toDF("edge_id", "serial_number", "ts10min","ts", "depth", "value")
        //  The primary key for "data" includes edge_id, serial_number, and ts.
        //   So, this is all records
        df
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "sensor_detail"))
          .save()

//         df.show(5)
        // println(s"${df.count()} rows processed.")
        //  The primary key for "last" is edge_id and sensor, this only holds the most recent
        //  values for the sensor.  Table handy for dashboards
        df
          .select("edge_id", "serial_number", "ts", "depth", "value")
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "last"))
          .save()
      }
    }

/*  remove for now
     this code works but not able to have two windows running in same routine so commented this out
  val windowMillis = 15000
  val sliderMillis = 5000
  val maRatio = windowMillis/(sliderMillis * 1.0)
//  this section writes out a count for each timestamp
  kafkaStream
        .window(Milliseconds(windowMillis), Milliseconds(sliderMillis))
    .foreachRDD {
      (message: RDD[(String, String)], batchTime: Time) => {
         val df = message.map {
          case (k, v) => v.split(";")
        }.map(payload => {
          val ts = Timestamp.valueOf(payload(3))
          val sensorMinuteFormat = new SimpleDateFormat("YYYYMMddHHmm")
          //   since want every 10 minutes, drop off the last minute place
          val currentMinute = sensorMinuteFormat.format(ts).dropRight(1)
          SensorEvent(payload(0), payload(1), currentMinute, ts, payload(4).toDouble, payload(5).toDouble)
        }).toDF("edge_id", "serial_number",  "ts10min","ts", "depth", "value")
          // gets the maximum timestamp for the window
        val maxTs = df.select("ts").sort(desc("ts")).first().get(0).asInstanceOf[Timestamp]
        val offsetTs = new Timestamp(maxTs.getTime - sliderMillis)
        val recCount = df.filter(df("ts") > offsetTs).count()
        val maRecCount = df.count()/maRatio
        //  The primary key for "count" is pk and ts.  So this keeps the count for each window and slider
        val dfCount = sc.makeRDD(Seq((1, maxTs, recCount, maRecCount))).toDF("pk", "ts", "count", "count_ma")
        dfCount.show()
        dfCount
          .write
          .format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> "demo", "table" -> "count"))
          .save()
      }
    }
    */

  //  this section writes max, min, sum, row count for each window
  val sensMap = kafkaStream.map { case (key, rawSensStr) =>
    val strings = rawSensStr.split(";")
    strings
  }
    sensMap.map(s => {val ts = Timestamp.valueOf(s(3))
      val sensorMinuteFormat = new SimpleDateFormat("YYYYMMddHHmm")
      //   since want every 10 minutes, drop off the last minute place
      val currentMinute = sensorMinuteFormat.format(ts).dropRight(1)
      s(1).toString -> ValStream(s(4).toFloat,s(5).toFloat,currentMinute
        // min
        ,s(4).toFloat,s(5).toFloat
        // sum
        ,s(4).toFloat,s(5).toFloat,1)})
    //  reduce to max temp,  max depth
    .reduceByKeyAndWindow({(x:ValStream,y:ValStream) => ValStream(math.max(x.max_depth,y.max_depth),
    math.max(x.max_value,y.max_value),x.ts10min,
    math.min(x.min_depth,y.min_depth), math.min(x.min_value,y.min_value),
      (x.sum_depth + y.sum_depth), (x.sum_value + y.sum_value),
    (x.row_count + y.row_count))}
    ,Seconds(600),Seconds(600))
  //   map to SumTableStream which has same structure as demo.sensor_summary.  Write to sensor_summary
      //    TTL can be defined in the table definition or here
    .map(s => {
    SumTableStream(s._1.toString,s._2.max_depth,s._2.max_value,s._2.ts10min
      ,s._2.min_depth,s._2.min_value
      ,s._2.sum_depth,s._2.sum_value,s._2.row_count)})
    .foreachRDD(a =>
      a.saveToCassandra("demo", "sensor_summary", SomeColumns("serial_number"
        , "max_depth", "max_value",  "ts10min", "min_depth", "min_value"
        ,  "sum_depth", "sum_value",  "row_count"),
        writeConf = WriteConf(ttl = TTLOption.constant(2592000)
        )
      ))
  //  Applied a TTL of 30 days = 2592000 sec. for sensor summary

  ssc.start()
  ssc.awaitTermination()
}
