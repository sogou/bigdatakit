package com.sogou.bigdatakit.hive.etl

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.{ConfigFactory, Config}

/**
  * Created by Tao Li on 2016/1/8.
  */
object HiveETLSettings {
  val DEFAULT_ROOT_KEY = "root.hive.etl"
  val DEFAULT_DATABASE = "custom"
  val DEFAULT_PARALLELISM = 1
}

class HiveETLSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val sparkSettings = new SparkSettings(config, args)
  @transient val conf = config.getConfig(HiveETLSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val DATABASE = conf.withFallback(
    ConfigFactory.parseMap(Map("database" -> HiveETLSettings.DEFAULT_DATABASE))
  ).getString("database")
  val TABLE = conf.getString("table")
  val PROCESSOR_CLASS = conf.getString("processor")
  val PARALLELISM = conf.withFallback(
    ConfigFactory.parseMap(Map("parallelism" -> HiveETLSettings.DEFAULT_PARALLELISM.asInstanceOf[Integer]))
  ).getInt("parallelism")

  val SPARK_MASTER_URL = conf.getString("master")
  val SPARK_APP_NAME = conf.withFallback(
    ConfigFactory.parseMap(Map("name" -> s"ETL-$DATABASE.$TABLE"))
  ).getString("name")

  import scala.collection.JavaConversions._

  val sparkConfigMap = sparkSettings.sparkConfigMap

  for (entry <- conf.entrySet()) {
    val k = entry.getKey
    val v = entry.getValue.atPath(k).getString(k)
    if (k.startsWith("spark.")) {
      sparkConfigMap.put(k, v)
    }
  }
}
