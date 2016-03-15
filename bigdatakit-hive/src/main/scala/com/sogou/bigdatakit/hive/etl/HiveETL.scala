package com.sogou.bigdatakit.hive.etl

import com.sogou.bigdatakit.hive.etl.processor.HiveETLProcessor
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Tao Li on 2016/1/8.
  */
object HiveETL {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("logdate is needed")
      System.exit(1)
    }

    val config = ConfigFactory.load()
    val settings = new HiveETLSettings(config, args)

    val logdate = args(0)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"${settings.SPARK_APP_NAME}-$logdate").setMaster(settings.SPARK_MASTER_URL)
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    val processor = Class.forName(settings.PROCESSOR_CLASS).newInstance.asInstanceOf[HiveETLProcessor]
    processor.run(sqlContext, settings.DATABASE, settings.TABLE, logdate, settings.PARALLELISM)
  }
}
