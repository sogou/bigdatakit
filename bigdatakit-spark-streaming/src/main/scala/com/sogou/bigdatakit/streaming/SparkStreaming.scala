package com.sogou.bigdatakit.streaming

import com.typesafe.config.ConfigFactory

/**
 * Created by Tao Li on 12/13/15.
 */
object SparkStreaming {
  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    val settings = new SparkStreamingSettings(config, args)

    val driver = settings.KAFKA_APPROACH match {
      case "receiver-based" => new ReceiverBasedSparkStreamingDriver(settings)
      case "direct-based" => new DirectBasedSparkStreamingDriver(settings)
      case other => throw new RuntimeException(s"no such kafka approach: $other}")
    }

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run = driver.stop
    }))

    driver.start
  }
}

