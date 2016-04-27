package com.sogou.bigdatakit.etl.phoenix

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by Tao Li on 2016/1/8.
  */
object PhoenixETLSettings {
  val DEFAULT_ROOT_KEY = "root.etl.phoenix"
  val DEFAULT_PARALLELISM = 1
}

class PhoenixETLSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val sparkSettings = new SparkSettings(config, args)
  @transient val conf = config.getConfig(PhoenixETLSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val TABLE = conf.getString("table")
  val PROCESSOR_CLASS = conf.getString("processor")
  val PARALLELISM = conf.withFallback(
    ConfigFactory.parseMap(Map("parallelism" -> PhoenixETLSettings.DEFAULT_PARALLELISM.asInstanceOf[Integer]))
  ).getInt("parallelism")

  val SPARK_MASTER_URL = conf.getString("master")
  val SPARK_APP_NAME = conf.withFallback(
    ConfigFactory.parseMap(Map("name" -> s"PhoenixETL-$TABLE"))
  ).getString("name")

  val sparkConfigMap = sparkSettings.sparkConfigMap
  val hadoopConfigMap = sparkSettings.hadoopConfigMap

  for (entry <- conf.entrySet()) {
    val k = entry.getKey
    val v = entry.getValue.atPath(k).getString(k)
    if (k.startsWith("spark.")) {
      sparkConfigMap.put(k, v)
    }
    if(k.startsWith("hadoop.")) {
      hadoopConfigMap.put(k.substring(7, k.length), v)
    }
  }
}
