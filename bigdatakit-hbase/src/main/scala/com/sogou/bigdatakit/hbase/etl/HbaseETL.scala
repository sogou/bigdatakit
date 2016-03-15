package com.sogou.bigdatakit.hbase.etl

import com.sogou.bigdatakit.common.util.AvroUtils
import com.sogou.bigdatakit.hbase.etl.processor.HbaseETLProcessor
import com.typesafe.config.ConfigFactory
import org.apache.avro.generic.GenericRecord
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Tao Li on 2016/3/15.
  */
object HbaseETL {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("logdate is needed")
      System.exit(1)
    }

    val config = ConfigFactory.load()
    val settings = new HbaseETLSettings(config, args)

    val logdate = args(0)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"${settings.SPARK_APP_NAME}-$logdate").setMaster(settings.SPARK_MASTER_URL)
    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    val processor = Class.forName(settings.PROCESSOR_CLASS).newInstance.
      asInstanceOf[HbaseETLProcessor]

    import unicredit.spark.hbase._

    implicit val hbaseConfig = HBaseConfig()

    val rdd = processor.doETL(sqlContext, settings.NAMESPACE, settings.TABLE, logdate).
      coalesce(settings.PARALLELISM)

    implicit val avroWriter = new Writes[GenericRecord] {
      def write(data: GenericRecord) = AvroUtils.avroObjectToBytes(data)
    }

    settings.APPROACH match {
      case "put" => rdd.toHBase(s"${settings.NAMESPACE}:${settings.TABLE}")
      case "bulkload" => rdd.toHBaseBulk(s"${settings.NAMESPACE}:${settings.TABLE}")
      case other => throw new RuntimeException(s"no such hbase import approach: $other}")
    }
  }
}