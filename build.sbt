name := "druid-indexing-on-spark"

version := "0.1"

scalaVersion := "2.12.5"

val druidVersion = "0.12.0"

libraryDependencies += "io.druid" % "druid-processing" % druidVersion
libraryDependencies += "io.druid" % "druid-server" % druidVersion
libraryDependencies += "io.druid" % "druid-indexing-service" % druidVersion
libraryDependencies += "io.druid" % "druid-indexing-hadoop" % druidVersion
