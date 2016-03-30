package com.sogou.bigdatakit.etl

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/3/30.
  */
trait DataFrameTransformer extends ETLProcessor[DataFrame] with java.io.Serializable {
  def transform(@transient sqlContext: HiveContext, logdate: String): DataFrame

  override def doETL(sqlContext: HiveContext, logdate: String): DataFrame = {
    transform(sqlContext, logdate)
  }
}
