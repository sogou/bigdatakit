package com.sogou.bigdatakit.etl.hbase

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Tao Li on 2016/3/15.
  */
object HBaseETL {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("logdate is needed")
      System.exit(1)
    }

    val config = ConfigFactory.load()
    val settings = new HBaseETLSettings(config, args)

    val logdate = args(0)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"${settings.SPARK_APP_NAME}-$logdate").setMaster(settings.SPARK_MASTER_URL)
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    Class.forName(settings.PROCESSOR_CLASS).newInstance match {
      case processor: HBaseAvroTransformer =>
        val rdd = processor.transform(sqlContext, logdate)
        settings.APPROACH match {
          case "put" => HBaseETLUtils.toHbase(rdd, settings.TABLE, settings.PARALLELISM)
          case other => throw new RuntimeException(s"not support approach: $other}")
        }
      case processor: HBaseCFAvroTransformer =>
        val rdd = processor.transform(sqlContext, logdate)
        settings.APPROACH match {
          case "put" => HBaseETLUtils.toHbase(rdd, settings.TABLE, settings.COLUMN_FAMILY, settings.PARALLELISM)
          case "bulkload" => HBaseETLUtils.toHbaseBulk(rdd, settings.TABLE, settings.COLUMN_FAMILY, settings.PARALLELISM)
          case other => throw new RuntimeException(s"not support approach: $other}")
        }
      case processor: HBaseETLRunner => processor.run(sqlContext, logdate)
      case _ => throw new RuntimeException(s"not support processor: ${settings.PROCESSOR_CLASS}")
    }
  }
}