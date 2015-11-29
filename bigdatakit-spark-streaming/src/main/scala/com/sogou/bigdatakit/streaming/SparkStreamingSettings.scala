package com.sogou.bigdatakit.streaming

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.{ConfigFactory, Config}

import scala.collection.mutable

/**
 * Created by Tao Li on 9/30/15.
 */
object SparkStreamingSettings {
  val DEFAULT_ROOT_KEY = "root.sparkStreaming"
}

class SparkStreamingSettings(config: Config, args: Array[String]) extends Serializable {
  @transient val sparkSettings = new SparkSettings(config, args)
  @transient val conf = config.getConfig(SparkStreamingSettings.DEFAULT_ROOT_KEY)

  import scala.collection.JavaConversions._

  val KAFKA_ZOOKEEPER_QUORUM = conf.getString("zkConnString")
  val KAFKA_TOPICS = conf.getString("topics")
  val KAFKA_CONSUMER_GROUP = conf.withFallback(
    ConfigFactory.parseMap(Map("groupId" -> s"default-$KAFKA_TOPICS"))
  ).getString("groupId")
  val KAFKA_CONSUMER_THREAD_NUM = conf.getInt("threadNum")

  val SPARK_MASTER_URL = conf.getString("master")
  val SPARK_APP_NAME = conf.getString("name")
  val BATCH_DURATION_SECONDS = conf.getLong("batchDuration")

  val PROCESSOR_CLASS = conf.getString("processor")


  val kafkaConfigMap = new mutable.HashMap[String, String]()
  val sparkConfigMap = sparkSettings.sparkConfigMap

  for (entry <- conf.entrySet()) {
    val k = entry.getKey
    val v = entry.getValue.atPath(k).getString(k)

    if (k.startsWith("spark.")) {
      sparkConfigMap.put(k, v)
    }
    if (k.startsWith("kafka.")) {
      kafkaConfigMap.put(k.substring(6), v)
    }
  }
}