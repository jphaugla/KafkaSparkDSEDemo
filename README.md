#KafkaSparkDSEDemo
#  Updated for DSE 5.0

The purpose of this demo is to demonstrate a simple Kafka/Spark/Scala IOT streaming example.  So, this has a scala program to create "sensor-like" load as well as a spark streaming job to write this "sensor data" to DataStax.

In order to run this demo, It is assumed that you have the following installed and available on your local system.

  1. Datastax Enterprise 5.0
  2. Apache Kafka 0.10.0.1, I used the Scala 2.10 build
  3. git
  4. sbt

##Getting Started with Kafka
Use the steps below to setup up a local instance of Kafka for this example. This is based off of apache-kafka_2.10-0.10.0.1.

MAC helpful tips at https://oleweidner.com/blog/2015/getting-started-with-kafka-on-osx/ 
Ubuntu helpful tips at https://devops.profitbricks.com/tutorials/install-and-configure-apache-kafka-on-ubuntu-1604-1/
###1. Locate and download Apache Kafka

Kafka can be located at this URL: [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

You will want to download and install the binary version for Scala 2.10.


###2. Install Apache Kafka

Once downloaded you will need to extract the file. It will create a folder/directory. Move this to a location of your choice.

**** (on mac)
brew install kafka 
pip install kafka-python 
**** (on ubuntu)
sudo apt-get install zookeeperd
wget http://mirror.fibergrid.in/apache/kafka/0.10.1.0/kafka_2.11-0.10.1.0.tgz
sudo mkdir /opt/Kafkax
cd /opt/Kafka
sudo tar -xvf ~datastax/kafka_2.10-0.10.1.0.tgz -C /opt/Kafka

###3. Start ZooKeeper and Kafka
Start local copy of zookeeper

####  on Mac
  * `<kafka home dir>bin/zookeeper-server-start config/zookeeper.properties`
or 
  * zkServer start
  * `kafka-server-start  /usr/local/etc/kafka/server.properties`

####  on Ubuntu, add Kafka to user $PATH
sudo /opt/Kafka/kafka_2.10-0.10.1.0/bin/kafka-server-start.sh /opt/Kafka/kafka_2.10-0.10.1.0/config/server.properties
(zookeeper automatically starts on install)

###4. Prepare a message topic for use.

Create the topic we will use for the demo

####  on ubuntu, command is kafka-topics.sh (sh suffix needed)
  * `kafka-topics --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic stream_ts`

Validate the topic was created. 

  * `kafka-topics --zookeeper localhost:2181 --list`
  
##A Couple of other useful Kafka commands
####  on ubuntu, commands need sh suffix 

Delete the topic. (Note: The server.properties file must contain `delete.topic.enable=true` for this to work)

  * `kafka-topics --zookeeper localhost:2181 --delete --topic stream_ts`
  
Show all of the messages in a topic from the beginning

  * `kafka-console-consumer --zookeeper localhost:2181 --topic stream_ts --from-beginning`
  
#Getting Started with Local DSE/Cassandra

###1. Download and install Datastax Enterprise v5.x

  * `https://academy.datastax.com/downloads/welcome`

###2. Starting DSE tarball install on the local OSX or Linux machine (-s starts search, -k starts Spark)

  * `dse cassandra -k -s` 
  
##Getting and running the demo

###In order to run this demo you will need to download the source from GitHub.

  * Navigate to the directory where you would like to save the code.
  * Execute the following command:
  
  
       `git clone git@github.com:jphaugla/KafkaSparkDSEDemo.git`
  
###To build the demo

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
