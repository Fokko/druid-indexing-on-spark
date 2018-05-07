package com.ing.wbaa

import com.google.common.hash.Hashing
import io.druid.indexer.InputRowSerde
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.joda.time.DateTime
import org.apache.spark.sql.functions._
import org.joda.time.DateTime
import org.apache.spark.util.SizeEstimator
import org.apache.spark.sql.Row

object DruidSparkIndexer extends App {

  case class Segment(shardNum: Int, dateTime: Long, partitionNum: Int)

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

  val bucketBy = millisPerHour

  val partitionStart = udf((time: String) => (new DateTime(time).getMillis / bucketBy).floor)
  val estimateSize = udf((row: Row) => SizeEstimator.estimate(row))
  val numSegments = udf((sizeInBytes: Long) => (sizeInBytes / targetSizeInBytes).ceil)
  val assignBucket = udf((numSegments: Long) => (numSegments * Math.random()).ceil)

  val rolledUpDf = inputDf.withColumn("__time", partitionStart('time))
    .withColumn("__size", estimateSize(struct(inputDf.columns.map(col): _*)))
    .cache()

  val partitionsDf = rolledUpDf.groupBy("__time")
    .sum("__size")
    .withColumn("num_segments", numSegments(col("sum(__size)")))

  partitionsDf.join(rolledUpDf, "__time")
    .withColumn("bucket", assignBucket('num_segments))
    .groupBy("__time", "bucket")
    .sum("__size")
    .show


//  val window = Window
//    .partitionBy("shardNum", "dateTime", "partitionNum")
//    .orderBy(WikitickerConfig.dimension.map(d => col(d)):_*)
//
//  val out = rolledUpDf.map(row => {
//    val sparkRow = new SparkBasedInputRow(row)
//
//    val serializedInputRow = InputRowSerde
//      .toBytes(InputRowSerde.getTypeHelperMap(
//        WikitickerConfig.dimensionSpec
//      ),
//        sparkRow,
//        Array()
//      )
//
//    val timestamp = new DateTime(row.getAs[String](WikitickerConfig.timeDimension)).getMillis
//
//    Segment(0, timestamp, 0) -> serializedInputRow
    //
    //    (
    //      new SortableBytes(
    //        new Bucket(0, new DateTime("2015-09-12T00:47:08Z"), 0).toGroupKey(),
    //        // sort rows by truncated timestamp and hashed dimensions to help reduce spilling on the reducer side
    //        ByteBuffer.allocate(java.lang.Long.BYTES + hashedDimensions.length)
    //          .putLong(timestamp)
    //          .put(hashedDimensions)
    //          .array()
    //      ),
    //      serializedInputRow
    //    ).toString()
//  }).groupByKey(row => {
//    val segment = row._1
//

}
