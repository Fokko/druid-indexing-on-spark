package com.ing.wbaa

import io.druid.data.input.InputRow
import io.druid.data.input.Row
import java.util
import org.apache.spark.sql.{Row => SparkRow}
import org.joda.time.DateTime
import scala.collection.JavaConversions._


class SparkBasedInputRow(row: SparkRow) extends InputRow {
  override def getDimensions: util.List[String] = {
    WikitickerConfig.dimension.filter(e => row.getAs[Object](e) != null)
  }

  override def getTimestampFromEpoch: Long = getTimestamp.getMillis

  override def getTimestamp: DateTime = {
    DateTime.parse(row.getAs[String](WikitickerConfig.timeDimension))
  }

  override def getDimension(dimension: String): util.List[String] = {
    List(row.getAs[Object](dimension).toString)
  }

  override def getRaw(dimension: String): AnyRef = {
    row.getAs[AnyRef](dimension)
  }

  override def getMetric(metric: String): Number = {
    row.getAs[Int](metric)
  }

  override def compareTo(o: Row): Int = o.compareTo(this)

  override def toString: String = {
    getTimestamp.toString()
  }
}
