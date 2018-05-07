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
    .json("/Users/fokkodriesprong/Desktop/docker-druid/ingestion/wikiticker-2015-09-12-sampled.json")

  val rolledUpDf = inputDf.withColumn("__time", bucket('time))
    .withColumn("__size", estimateSize(struct(inputDf.columns.map(col): _*)))
    .cache()

  rolledUpDf.groupBy("__time")
    .sum("__size")
    .withColumn("num_segments", numSegments(col("sum(__size)")))
    .show

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
