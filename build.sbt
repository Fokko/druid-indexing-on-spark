name := "druid-indexing-on-spark"

version := "0.1"

scalaVersion := "2.11.12"

val druidVersion = "0.13.0-SNAPSHOT"
val sparkVersion = "2.3.0"

resolvers += Resolver.mavenLocal

//libraryDependencies += "io.druid" % "druid-processing" % druidVersion excludeAll
//libraryDependencies += "io.druid" % "druid-server" % druidVersion excludeAll
//libraryDependencies += "io.druid" % "druid-indexing-service" % druidVersion excludeAll
libraryDependencies += "io.druid" % "druid-indexing-hadoop" % druidVersion

libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion
