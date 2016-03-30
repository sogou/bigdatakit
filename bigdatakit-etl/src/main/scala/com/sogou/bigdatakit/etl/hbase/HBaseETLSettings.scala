package com.sogou.bigdatakit.etl.hbase

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by Tao Li on 2016/1/8.
  */
object HBaseETLSettings {
  val DEFAULT_ROOT_KEY = "root.etl.hbase"
  val DEFAULT_NAMESPACE = "default"
  val DEFAULT_TABLE = "test"
  val DEFAULT_COLUMN_FAMILY = "cf"
  val DEFAULT_APPROACH = "put"
  val DEFAULT_PARALLELISM = 1
}

class HBaseETLSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val sparkSettings = new SparkSettings(config, args)
  @transient val conf = config.getConfig(HBaseETLSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val PROCESSOR_CLASS = conf.getString("processor")
  val NAMESPACE = conf.withFallback(
    ConfigFactory.parseMap(Map("namespace" -> HBaseETLSettings.DEFAULT_NAMESPACE))
  ).getString("namespace")
  val TABLE = conf.withFallback(
    ConfigFactory.parseMap(Map("table" -> HBaseETLSettings.DEFAULT_TABLE))
  ).getString("table")
  val COLUMN_FAMILY = conf.withFallback(
    ConfigFactory.parseMap(Map("columnFamily" -> HBaseETLSettings.DEFAULT_COLUMN_FAMILY))
  ).getString("columnFamily")
  val APPROACH = conf.withFallback(
    ConfigFactory.parseMap(Map("approach" -> HBaseETLSettings.DEFAULT_APPROACH))
  ).getString("approach")
  val PARALLELISM = conf.withFallback(
    ConfigFactory.parseMap(Map("parallelism" -> HBaseETLSettings.DEFAULT_PARALLELISM.asInstanceOf[Integer]))
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
