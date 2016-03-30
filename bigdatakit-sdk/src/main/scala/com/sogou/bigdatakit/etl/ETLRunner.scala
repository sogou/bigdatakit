package com.sogou.bigdatakit.etl

import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/3/30.
  */
trait ETLRunner extends ETLProcessor[Unit] with java.io.Serializable {
  def run(@transient sqlContext: HiveContext, logdate: String)

  override def doETL(sqlContext: HiveContext, logdate: String): Unit = {
    run(sqlContext, logdate)
  }
}
