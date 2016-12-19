val globalSettings = Seq(
  version := "0.1",
  scalaVersion := "2.10.5"
)

val akkaVersion = "2.3.12"
//  use this for DSE 4.8.x
// val sparkVersion = "1.4.1"
val sparkVersion = "1.6.1"

//  use this for DSE 4.8.x
// val sparkCassandraConnectorVersion = "1.4.0"
val sparkCassandraConnectorVersion = "1.6.1-s_2.10"
// val kafkaVersion = "0.8.2.2"
val kafkaVersion = "0.10.0.1"
val scalaTestVersion = "2.2.4"


lazy val producer = (project in file("producer"))
  .settings(name := "producer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= producerDeps)

lazy val consumer = (project in file("consumer"))
  .settings(name := "consumer")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= consumerDeps)


lazy val producerDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.apache.kafka" % "kafka_2.10" % kafkaVersion
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

lazy val consumerDeps = Seq(
  "datastax" % "spark-cassandra-connector" % sparkCassandraConnectorVersion,
  "org.apache.spark"  %% "spark-mllib"           % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-graphx"          % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-sql"             % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-streaming"       % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-streaming-kafka" % sparkVersion % "provided"
// , "databricks"    %% "spark-csv"             % sparkVersion 
)
    
