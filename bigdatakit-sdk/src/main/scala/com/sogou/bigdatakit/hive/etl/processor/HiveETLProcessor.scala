package com.sogou.bigdatakit.hive.etl.processor

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Created by Tao Li on 2016/1/8.
  */
abstract class HiveETLProcessor extends java.io.Serializable {
  def dropPartition(@transient sqlContext: HiveContext,
                    database: String, table: String, logdate: String) = {
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table drop partition (logdate=$logdate)")
  }

  def saveToPartiton(@transient sqlContext: HiveContext, df: DataFrame,
                     database: String, table: String, logdate: String, parallelism: Int) = {
    val warehouseRootDir: String = "hdfs://SunshineNameNode2/user/hive/warehouse"
    val tableLocation = s"$warehouseRootDir/$database.db/$table/logdate=$logdate"
    df.coalesce(parallelism).write.mode(SaveMode.Append).format("orc").save(tableLocation)
    sqlContext.sql(s"dfs -chmod a+w $tableLocation")
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table add partition (logdate=$logdate) location '$tableLocation'")
  }

  def doETL(@transient sqlContext: HiveContext,
            database: String, table: String, logdate: String): DataFrame

  def run(@transient sqlContext: HiveContext,
          database: String, table: String, logdate: String, parallelism: Int) = {
    dropPartition(sqlContext, database, table, logdate)
    val df = doETL(sqlContext, database, table, logdate)
    saveToPartiton(sqlContext, df, database, table, logdate, parallelism)
  }
}