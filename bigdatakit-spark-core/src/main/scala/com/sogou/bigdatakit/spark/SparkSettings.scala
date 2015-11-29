package com.sogou.bigdatakit.spark

import com.typesafe.config.Config

import scala.collection.mutable

/**
 * Created by Tao Li on 9/30/15.
 */
object SparkSettings {
  val DEFAULT_ROOT_KEY = "root.spark"
}

class SparkSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val conf = config.getConfig(SparkSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val sparkConfigMap = new mutable.HashMap[String, String]()

  for (entry <- conf.entrySet()) {
    val k = entry.getKey
    val v = entry.getValue.atPath(k).getString(k)

    if (k.startsWith("spark.")) {
      sparkConfigMap.put(k, v)
    }
  }
}