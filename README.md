#KafkaSparkDSEDemo
#  Updated for DSE 5.0

In order to run this demo, It is assumed that you have the following installed and available on your local system.

  1. Datastax Enterprise 5.0
  2. Apache Kafka 0.10.0.1, I used the Scala 2.10 build
  3. git
  4. sbt

##Getting Started with Kafka
Use the steps below to setup up a local instance of Kafka for this example. This is based off of apache-kafka_2.10-0.10.0.1.

###1. Locate and download Apache Kafka

Kafka can be located at this URL: [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

You will want to download and install the binary version for Scala 2.10.

###2. Install Apache Kafka

Once downloaded you will need to extract the file. It will create a folder/directory. Move this to a location of your choice.

###3. Start ZooKeeper and Kafka

Start local copy of zookeeper

  * `<kafka home dir>bin/zookeeper-server-start config/zookeeper.properties`
or 
  * zkServer start

Start local copy of Kafka

  * `kafka-server-start  /usr/local/etc/kafka/server.properties`

###4. Prepare a message topic for use.

Create the topic we will use for the demo

  * `kafka-topics --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic stream_ts`

Validate the topic was created. 

  * `kafka-topics --zookeeper localhost:2181 --list`
  
##A Couple of other useful Kafka commands

Delete the topic. (Note: The server.properties file must contain `delete.topic.enable=true` for this to work)

  * `kafka-topics --zookeeper localhost:2181 --delete --topic stream_ts`
  
Show all of the messages in a topic from the beginning

  * `kafka-console-consumer --zookeeper localhost:2181 --topic stream_ts --from-beginning`
  
#Getting Started with Local DSE/Cassandra

###1. Download and install Datastax Enterprise v5.x

  * `https://academy.datastax.com/downloads/welcome`

###2. Starting DSE tarball install on the local OSX or Linux machine

  * `dse cassandra -k -s` 
  
##Getting and running the demo

###In order to run this demo you will need to download the source from GitHub.

  * Navigate to the directory where you would like to save the code.
  * Execute the following command:
  
  
       `git clone https://github.com/CaryBourgeois/KafkaSparkCassandraDemo.git`
  
###To build the demo

  * Create the Cassandra Keyspaces and Tables using the `CreateTable.cql` script
  * Navigate to the root directory of the project where you downloaded
  * Build the Producer with this command:
  
    `sbt producer/package`
      
  * Build the Consumer with this command:
  
    `sbt consumer/package`

  * Create cql tables
     *  NOTE:  demo keyspace is created with SimpleStrategy-change this if running on more than one node!

    `cqlsh -f consumer/resources/cql/CreateTables.cql`
  
  * load the sensor meta data
   
    `cqlsh -f consumer/resources/cql/loaddata.cql`

###To run the demo

This assumes you already have Kafka and DSE up and running and configured as in the steps above.

  * From the root directory of the project start the producer app
  
    `sbt producer/run`
    
  
  * From the root directory of the project start the consumer app
  
    `dse spark-submit --packages org.apache.spark:spark-streaming-kafka-assembly_2.10:1.6.1 --class com.datastax.demo.SparkKafkaConsumer consumer/target/scala-2.10/consumer_2.10-0.1.jar`
  
  *  From the root directory of the project can do one-time aggregate to sensor_full_summary.  To rerun, truncate, the demo.sensor_full_summary table.
    `dse spark-submit --packages org.apache.spark:spark-streaming-kafka-assembly_2.10:1.6.1 --class SensorAggregates consumer/target/scala-2.10/consumer_2.10-0.1.jar`
