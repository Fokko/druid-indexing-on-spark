package com.ing.wbaa

import com.google.common.hash.Hashing
import org.apache.spark.sql.SparkSession


object DruidSparkIndexer extends App {

  val hashFunction = Hashing.murmur3_128

  val spark = SparkSession
    .builder()
    .appName("DruidSparkIndexer")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  val df = spark
    .read
    .json("/Users/fokkodriesprong/Desktop/docker-druid/ingestion/wikiticker-2015-09-12-sampled.json")


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
