package com.sogou.bigdatakit.etl

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/3/30.
  */
trait RDDTransformer[T] extends ETLProcessor[RDD[T]] with java.io.Serializable {
  def transform(@transient sqlContext: HiveContext, logdate: String): RDD[T]

  override def doETL(sqlContext: HiveContext, logdate: String): RDD[T] = {
    transform(sqlContext, logdate)
  }
}
