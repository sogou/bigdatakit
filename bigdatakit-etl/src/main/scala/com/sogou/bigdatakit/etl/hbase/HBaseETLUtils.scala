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

  class AvroWrites[T <: SpecificRecordBase] extends Writes[T] {
    override def write(data: T): Array[Byte] = AvroUtils.avroObjectToBytes(data)
  }

  def toHbase[T <: SpecificRecordBase](rdd: RDD[(String, Map[String, Map[String, (T, Long)]])],
                                       table: String): Unit = {
    implicit val avroWriter = new AvroWrites[T]
    rdd.toHBase(table)
  }

  def toHbase[T <: SpecificRecordBase](rdd: RDD[(String, Map[String, (T, Long)])],
                                       table: String, cf: String): Unit = {
    implicit val avroWriter = new AvroWrites[T]
    rdd.toHBase(table, cf)
  }

  def toHbaseBulk[T <: SpecificRecordBase](rdd: RDD[(String, Map[String, (T, Long)])],
                                           table: String, cf: String): Unit = {
    implicit val avroWriter = new AvroWrites[T]
    rdd.toHBaseBulk(table, cf)
  }
}