package com.sogou.bigdatakit.etl.phoenix

import com.sogou.bigdatakit.etl.hive.HiveETLSettings
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Created by Tao Li on 2016/3/30.
  */
object PhoenixETLUtils {
  val conf = HBaseConfiguration.create()
  val zkUrl = conf.get("hbase.zookeeper.quorum")

  def toPhoenix(df: DataFrame, table: String,
                parallelism: Int = HiveETLSettings.DEFAULT_PARALLELISM): Unit = {
    df.coalesce(parallelism).write.
      format("org.apache.phoenix.spark").
      mode(SaveMode.Overwrite).
      options(
        Map("table" -> table, "zkUrl" -> zkUrl)
      ).save()
  }
}