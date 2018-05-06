package com.ing.wbaa

import io.druid.data.input.impl.DimensionSchema

// This should be read from a nice config file
object WikitickerConfig {
  val timeDimension = "time"

  val dimension = Array(
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

  dimension.map(dim => new DimensionSchema(dim, null, true))
}
