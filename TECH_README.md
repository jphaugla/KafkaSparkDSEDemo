# KafkaSparkDSEDemo
  Updated for DSE 6.x with Spark 2.1.2 and structured streaming
  Updated for DSE 6.x which used Spark 2.0.2
  Also updated for standalone Spark 2.0.2
  Added python version of code base in consumer/src/main/python
  Added fat jar option using sbt-assembly

The purpose of this demo is to demonstrate a simple Kafka/Spark/Scala IOT streaming example.  So, this has a scala program to create "sensor-like" load as well as a spark streaming job to write this "sensor data" to DataStax.

In order to run this demo, It is assumed that you have the following installed and available on your local system.

  1. Datastax Enterprise 6.x
  2. Apache Kafka 1.1.0, Scala 2.11 build
  3. git
  4. sbt

## Getting started with Kafka
Use the steps below to setup up a local instance of Kafka for this example. This is based off of kafka_2.11-1.1.0.tgz


Ubuntu helpful tips at https://devops.profitbricks.com/tutorials/install-and-configure-apache-kafka-on-ubuntu-1604-1/ 

### Locate and download Apache Kafka

Kafka can be located at this URL: 
	[http://kafka.apache.org/downloads.html](http://kafka.apache.org/downloads.html)

download and install the binary version for Scala 2.11.

###  install sbt

#### on ubuntu 

	echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
	sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
	sudo apt-get update
	sudo apt-get install sbt

#### on mac 
	brew install sbt

### Download and install Datastax Enterprise v6.x.x

  * `https://academy.datastax.com/downloads/welcome`

### Install Apache Kafka and Zookeeper

Once downloaded you will need to extract the file. It will create a folder/directory. Move this to a location of your choice.

#### (on mac)

	brew install kafka
	pip install kafka-python

verify the correct kafka in path from brew
        `which kafka-topics` should return "/usr/local/bin/kafka-topics"

double check the link is correct in /usr/local/bin for desired version

        `ls -lrt /usr/local/bin/kafka-topics` should point to "../Cellar/kafka/1.1.0/bin/kafka-topics"

#### (on ubuntu)

	sudo apt-get install zookeeperd
	wget http://apache.claz.org/kafka/0.10.2.0/kafka_2.11-1.1.0.tgz
	sudo mkdir /opt/Kafka
	cd /opt/Kafka
	sudo tar -xvf ~datastax/kafka_2.11-1.1.0.tgz -C /opt/Kafka

for convenience, created a soft link to /opt/Kafka/kafka 

	cd /opt/Kafka
	ln -s kafka_2.11-1.1.0 kafka

#### for kafka and python on ubuntu 

	sudo apt-get install python-pip python-dev build-essential
	sudo pip install kafka-python

### Start ZooKeeper and Kafka
Start local copy of zookeeper and Kafka

####  on Mac
this starts zookeeper
  * zkServer start
ithis starts kafka
  * `kafka-server-start  /usr/local/etc/kafka/server.properties`

####  on Ubuntu

	add kafka bin to the PATH in your profile
           echo "export PATH=/opt/Kafka/kafka/bin:$PATH" >> ~/.bashrc
	sudo /opt/Kafka/kafka/bin/kafka-server-start.sh /opt/Kafka/kafka/config/server.properties

(zookeeper automatically starts on install)

moving forward, manage zookeeper on ubuntu with "service zookeeper status"

### Prepare a message topic for use.

Create the topic we will use for the demo

###  on mac, command is kafka-topics (sh suffix not needed)
  * `kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic stream_ts`
  * `kafka-topics.sh --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic full_summary`

Validate the topic was created. 

  * `kafka-topics.sh --zookeeper localhost:2181 --list`
  
### A Couple of other useful Kafka commands
####  on ubuntu, commands need sh suffix 

Delete the topic. (Note: The server.properties file must contain `delete.topic.enable=true` for this to work)

  * `kafka-topics.sh --zookeeper localhost:2181 --delete --topic stream_ts`
  
Show all of the messages in a topic from the beginning

  * `kafka-console-consumer.sh --zookeeper localhost:2181 --topic stream_ts --from-beginning`
  
# Getting Started with Local DSE/Cassandra


### Starting DSE tarball install on the local OSX or Linux machine (-s starts search, -k starts Spark)
    search could be easily incorporated to this demo using the sensor_full_summary table

  * `dse cassandra -k -s` 
  
## Getting and running the demo

### In order to run this demo you will need to download the source from GitHub.

  * Navigate to the directory where you would like to save the code.
  * Execute the following command:
  
       `git clone git@github.com:jphaugla/KafkaSparkDSEDemo.git`
  
  * Create cql tables
     *  NOTE:  demo keyspace is created with SimpleStrategy-change this if running on more than one node!

    `cqlsh -f consumer/resources/cql/CreateTables.cql`
  
  * load the sensor meta data
   
    `cqlsh -f consumer/resources/cql/loaddata.cql`

### To build the demo

    to do standalone spark switch the build.sbt to build.sbt.spark2
     otherwise, this is set up for embedded datastax

  * Navigate to the root directory of the project where you downloaded
  * Build the Producer with this command:
  
    `sbt producer/package`
      
  * Build the Consumer with this command:
  
    `sbt consumer/package`

   see note at bottom if errors here

### To run the demo

This assumes you already have Kafka and DSE up and running and configured as in the steps above.

  * From the root directory of the project start the producer app
  
	`sbt producer/run`
    
  
  * From the root directory of the project start the consumer app

	`./runConsumer.sh   

  * After running for some time can run aggregate to create sensor_full_summary

	`./runAggregate.sh   

  * Can write sensor_full_summary back to a full_summary kafka topic

	`./runWriteBack.sh  
  
####  PROBLEMS with build.sbt
Needed to clean out jar files on spark and dse dependencies

	rm -rf ~/.ivy2/cache/org.apache.spark/*
	rm -rf ~/.ivy2/cache/com.datastax.dse/*
