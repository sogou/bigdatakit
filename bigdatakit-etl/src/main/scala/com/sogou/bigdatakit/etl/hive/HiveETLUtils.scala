package com.sogou.bigdatakit.etl.hive

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Created by Tao Li on 2016/3/30.
  */
object HiveETLUtils {
  def dropPartition(@transient sqlContext: HiveContext,
                    database: String, table: String, logdate: String) = {
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table drop partition (logdate=$logdate)")
  }

  def saveToPartiton(@transient sqlContext: HiveContext, df: DataFrame,
                     database: String, table: String, logdate: String,
                     parallelism: Int = HiveETLSettings.DEFAULT_PARALLELISM) = {
    val warehouseRootDir: String = "hdfs://SunshineNameNode2/user/hive/warehouse"
    val tableLocation = s"$warehouseRootDir/$database.db/$table/logdate=$logdate"
    df.coalesce(parallelism).write.mode(SaveMode.Append).format("orc").save(tableLocation)
    sqlContext.sql(s"dfs -chmod a+w $tableLocation")
    sqlContext.sql(s"use $database")
    sqlContext.sql(s"alter table $table add partition (logdate=$logdate) location '$tableLocation'")
  }
}
