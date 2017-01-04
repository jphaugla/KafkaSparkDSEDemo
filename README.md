#KafkaSparkDSEDemo
#  Updated for DSE 5.0
#  Also updated for standalone Spark 2.0.2

The purpose of this demo is to demonstrate a simple Kafka/Spark/Scala IOT streaming example.  So, this has a scala program to create "sensor-like" load as well as a spark streaming job to write this "sensor data" to DataStax.

In order to run this demo, It is assumed that you have the following installed and available on your local system.

  1. Datastax Enterprise 5.0
  2. Apache Kafka 0.10.1.1, Scala 2.11 build
  3. git
  4. sbt

##Getting Started with Kafka
Use the steps below to setup up a local instance of Kafka for this example. This is based off of apache-kafka_2.11-0.10.1.1


Ubuntu helpful tips at https://devops.profitbricks.com/tutorials/install-and-configure-apache-kafka-on-ubuntu-1604-1/ 

###1. Locate and download Apache Kafka

Kafka can be located at this URL: [http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

You will want to download and install the binary version for Scala 2.11.


###2. Install Apache Kafka

Once downloaded you will need to extract the file. It will create a folder/directory. Move this to a location of your choice.

### (on mac)
brew install kafka 
pip install kafka-python 
### (on ubuntu)
sudo apt-get install zookeeperd

wget http://mirror.fibergrid.in/apache/kafka/0.10.1.1/kafka_2.11-0.10.1.1.tgz 

sudo mkdir /opt/Kafka

cd /opt/Kafka

sudo tar -xvf ~datastax/kafka_2.11-0.10.1.1.tgz -C /opt/Kafka

for convenience, created a soft link to /opt/Kafka/kafka 
cd /opt/Kafka
ln -s kafka_2.11-0.10.1.1 kafka

###3. Start ZooKeeper and Kafka
Start local copy of zookeeper

####  on Mac
  * `<kafka home dir>bin/zookeeper-server-start config/zookeeper.properties`
or 
  * zkServer start
  * `kafka-server-start  /usr/local/etc/kafka/server.properties`

####  on Ubuntu, add Kafka to user $PATH
sudo /opt/Kafka/kafka/bin/kafka-server-start.sh /opt/Kafka/kafka/config/server.properties
(zookeeper automatically starts on install)
moving forward, manage zookeeper on ubuntu with "service zookeeper status"

###4. Prepare a message topic for use.

Create the topic we will use for the demo

####  on ubuntu, command is kafka-topics.sh (sh suffix needed)
  * `kafka-topics --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic stream_ts`
  * `kafka-topics --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic full_summary`

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
    search could be easily incorporated to this demo using the sensor_full_summary table

  * `dse cassandra -k -s` 
  
##Getting and running the demo

###In order to run this demo you will need to download the source from GitHub.

  * Navigate to the directory where you would like to save the code.
  * Execute the following command:
  
  
       `git clone git@github.com:jphaugla/KafkaSparkDSEDemo.git`
  
  * Create cql tables
     *  NOTE:  demo keyspace is created with SimpleStrategy-change this if running on more than one node!

    `cqlsh -f consumer/resources/cql/CreateTables.cql`
  
  * load the sensor meta data
   
    `cqlsh -f consumer/resources/cql/loaddata.cql`

###  need to have sbt installed
#### on ubuntu apt-get install sbt
#### on mac brew install sbt
###To build the demo

#   to do standalone spark switch the build.sbt to build.sbt.spark2
#     otherwise, this is set up for embedded datastax

  * Navigate to the root directory of the project where you downloaded
  * Build the Producer with this command:
  
    `sbt producer/package`
      
  * Build the Consumer with this command:
  
    `sbt consumer/package`
#   see note at bottom if errors here

###To run the demo

This assumes you already have Kafka and DSE up and running and configured as in the steps above.

  * From the root directory of the project start the producer app
  
    `sbt producer/run`
    
  
  * From the root directory of the project start the consumer app

    ./runConsumer.sh   (if using DSE embedded 5.0.x)
    ./runConsumer2.sh  (if using standalone spark 2.0.2)

  * After running for some time can run aggregate to create sensor_full_summary
    ./runAggregate.sh   (if using DSE embedded 5.0.x)
    ./runAggregate2.sh  (if using standalone spark 2.0.2)

  * Can write sensor_full_summary back to a full_summary kafka topic
    ./runWriteBack.sh   (if using DSE embedded 5.0.x)
    ./runWriteBack2.sh  (if using standalone spark 2.0.2)
  
####  PROBLEMS with build.sbt
The cleaner new build.sbt did not work on my Mac running DSE 5.0.3 or DSE 5.0.5.  If see dependency problems switch back to build.sbt.mac and then use 
	runConsumer.sh.mac and runAggregate.sh.mac
