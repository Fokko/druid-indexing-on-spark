package com.ing.wbaa

import com.google.common.hash.Hashing
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.joda.time.DateTime
import org.apache.spark.util.SizeEstimator
import org.apache.spark.sql.Row

object DruidSparkIndexer extends App {

  val hashFunction = Hashing.murmur3_128

  val spark = SparkSession
    .builder()
    .appName("DruidSparkIndexer")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  val inputDf = spark
    .read
    .json("/Users/stijnkoopal/Documents/projects/ing/experimentation-day/druid-indexing-on-spark/wikiticker-2015-09-12-sampled.json")

  val targetSizeInBytes = 1 * 1024 * 1024

  val millisPerHour = 60 * 60 * 1000
  val millisPerDay = 24 * millisPerHour

  val bucket = udf((time: String) => (new DateTime(time).getMillis / millisPerHour).floor)
  val estimateSize = udf((row: Row) => SizeEstimator.estimate(row))
  val numSegments = udf((sizeInBytes: Long) => (sizeInBytes / targetSizeInBytes).ceil)

  val rolledUpDf = inputDf.withColumn("__time", bucket('time))
    .withColumn("__size", estimateSize(struct(inputDf.columns.map(col): _*)))
    .cache()

  val partitionsDf = rolledUpDf.groupBy("__time")
    .sum("__size")
    .withColumn("num_segments", numSegments(col("sum(__size)")))
//
//  partitionsDf.join(rolledUpDf, "__time")
//    .withColumn("bucket", "num_segments".)
//
//  df.map(row =>{
//    import io.druid.indexer.InputRowSerde
//    val row = new SparkBasedInputRow(row)
//
//
//
//
//      InputRowSerde.toBytes(typeHelperMap, row, aggregators, reportParseExceptions)
//  }
//
//
//  ).show(false)

}
