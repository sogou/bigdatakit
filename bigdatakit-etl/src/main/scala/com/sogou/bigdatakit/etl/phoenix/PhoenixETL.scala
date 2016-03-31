package com.sogou.bigdatakit.etl.phoenix

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Tao Li on 2016/3/15.
  */
object PhoenixETL {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("logdate is needed")
      System.exit(1)
    }

    val config = ConfigFactory.load()
    val settings = new PhoenixETLSettings(config, args)

    val logdate = args(0)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"${settings.SPARK_APP_NAME}-$logdate").setMaster(settings.SPARK_MASTER_URL)
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    val processor = Class.forName(settings.PROCESSOR_CLASS).newInstance.
      asInstanceOf[PhoenixTransformer]

    val df = processor.transform(sqlContext, logdate).coalesce(settings.PARALLELISM)
    PhoenixETLUtils.toPhoenix(df, settings.TABLE)
  }
}