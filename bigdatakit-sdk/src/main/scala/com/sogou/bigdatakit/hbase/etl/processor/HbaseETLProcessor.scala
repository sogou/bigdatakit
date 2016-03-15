package com.sogou.bigdatakit.hbase.etl.processor

import org.apache.avro.specific.SpecificRecordBase
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Tao Li on 2016/3/15.
  */
abstract class HbaseETLProcessor extends java.io.Serializable {
  def doETL(@transient sqlContext: HiveContext, namespace: String, table: String,
            logdate: String): RDD[(String, Map[String, (SpecificRecordBase, Long)])]
}
