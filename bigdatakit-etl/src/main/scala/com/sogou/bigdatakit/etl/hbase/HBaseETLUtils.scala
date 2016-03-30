package com.sogou.bigdatakit.etl.hbase

import com.sogou.bigdatakit.common.util.AvroUtils
import org.apache.avro.specific.SpecificRecordBase
import org.apache.spark.rdd.RDD
import unicredit.spark.hbase._

/**
  * Created by Tao Li on 2016/3/30.
  */
object HBaseETLUtils {
  implicit val hbaseConfig = HBaseConfig()
  
  implicit val avroWriter = new Writes[SpecificRecordBase] {
    def write(data: SpecificRecordBase) = AvroUtils.avroObjectToBytes(data)
  }

  def toHbase(rdd: RDD[(String, Map[String, Map[String, (SpecificRecordBase, Long)]])],
              table: String): Unit = {
    rdd.toHBase(table)
  }

  def toHbase(rdd: RDD[(String, Map[String, (SpecificRecordBase, Long)])],
              table: String, cf: String): Unit = {
    rdd.toHBase(table, cf)
  }

  def toHbaseBulk(rdd: RDD[(String, Map[String, (SpecificRecordBase, Long)])],
                  table: String, cf: String): Unit = {
    rdd.toHBaseBulk(table, cf)
  }
}
