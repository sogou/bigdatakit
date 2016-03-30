package com.sogou.bigdatakit.etl

import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/3/30.
  */
trait ETLProcessor[T] {
  def doETL(@transient sqlContext: HiveContext, logdate: String): T
}
