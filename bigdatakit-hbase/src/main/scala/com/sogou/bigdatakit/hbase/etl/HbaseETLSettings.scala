package com.sogou.bigdatakit.hbase.etl

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by Tao Li on 2016/1/8.
  */
object HbaseETLSettings {
  val DEFAULT_ROOT_KEY = "root.hbase.etl"
  val DEFAULT_NAMESPACE = "default"
  val DEFAULT_APPROACH = "put"
  val DEFAULT_PARALLELISM = 1
}

class HbaseETLSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val sparkSettings = new SparkSettings(config, args)
  @transient val conf = config.getConfig(HbaseETLSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val NAMESPACE = conf.withFallback(
    ConfigFactory.parseMap(Map("namespace" -> HbaseETLSettings.DEFAULT_NAMESPACE))
  ).getString("namespace")
  val TABLE = conf.getString("table")
  val PROCESSOR_CLASS = conf.getString("processor")
  val APPROACH = conf.withFallback(
    ConfigFactory.parseMap(Map("approach" -> HbaseETLSettings.DEFAULT_APPROACH))
  ).getString("approach")
  val PARALLELISM = conf.withFallback(
    ConfigFactory.parseMap(Map("parallelism" -> HbaseETLSettings.DEFAULT_PARALLELISM.asInstanceOf[Integer]))
  ).getInt("parallelism")

  val SPARK_MASTER_URL = conf.getString("master")
  val SPARK_APP_NAME = conf.withFallback(
    ConfigFactory.parseMap(Map("name" -> s"HbaseETL-$NAMESPACE.$TABLE"))
  ).getString("name")

  val sparkConfigMap = sparkSettings.sparkConfigMap

  for (entry <- conf.entrySet()) {
    val k = entry.getKey
    val v = entry.getValue.atPath(k).getString(k)
    if (k.startsWith("spark.")) {
      sparkConfigMap.put(k, v)
    }
  }
}
