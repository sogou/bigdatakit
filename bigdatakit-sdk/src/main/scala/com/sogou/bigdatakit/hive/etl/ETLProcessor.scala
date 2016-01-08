package com.sogou.bigdatakit.hive.etl

import org.apache.spark.sql.{SaveMode, DataFrame}
import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/1/8.
  */
abstract class ETLProcessor extends java.io.Serializable {
  def dropPartition(sqlContext: HiveContext, database: String, table: String, logdate: String) = {
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table drop partition (logdate=$logdate)")
  }

  def saveToPartiton(sqlContext: HiveContext, df: DataFrame, database: String, table: String, logdate: String) = {
    val warehouseRootDir: String = "hdfs://SunshineNameNode2/user/hive/warehouse"
    val tableLocation = s"$warehouseRootDir/$database.db/$table/logdate=$logdate"
    df.coalesce(1).write.mode(SaveMode.Append).format("orc").save(tableLocation)
    sqlContext.sql(s"dfs -chmod a+w $tableLocation")
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table add partition (logdate=$logdate) location '$tableLocation'")
  }

  def doETL(sqlContext: HiveContext, database: String, table: String, logdate: String): DataFrame

  def run(sqlContext: HiveContext, database: String, table: String, logdate: String) = {
    dropPartition(sqlContext, database, table, logdate)
    val df = doETL(sqlContext, database, table, logdate)
    saveToPartiton(sqlContext, df, database, table, logdate)
  }
}