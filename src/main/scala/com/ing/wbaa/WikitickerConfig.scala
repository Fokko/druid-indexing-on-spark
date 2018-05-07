package com.ing.wbaa

import io.druid.data.input.impl.DimensionsSpec
import scala.collection.JavaConversions._

// This should be read from a nice config file
object WikitickerConfig {
  val timeDimension = "time"

  val dimension = List(
    "channel",
    "cityName",
    "comment",
    "countryIsoCode",
    "countryName",
    "isAnonymous",
    "isMinor",
    "isNew",
    "isRobot",
    "isUnpatrolled",
    "metroCode",
    "namespace",
    "page",
    "regionIsoCode",
    "regionName",
    "user"
  )

  val dimensionSchema = DimensionsSpec.getDefaultSchemas(dimension)
    val dimensionSpec = new DimensionsSpec(dimensionSchema, List(), null)
}
