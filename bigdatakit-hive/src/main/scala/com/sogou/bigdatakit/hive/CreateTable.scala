package com.sogou.bigdatakit.hive

import java.io.File

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory

/**
  * Created by Tao Li on 2016/1/4.
  */
object CreateTable {
  val ORC_INPUTFORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat"
  val ORC_OUTPUTFORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat"
  val ORC_SERDE = "org.apache.hadoop.hive.ql.io.orc.OrcSerde"

  val SYMLINK_INPUTFORMAT = "com.sogou.datadir.plugin.SymlinkLzoTextInputFormat"
  val HIVE_IGNOREKEY_OUTPUTFORMAT = "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat"

  import scala.collection.JavaConversions._

  val DEFAULT_CONFIG = ConfigFactory.parseMap(Map("serde" -> "orc"))

  private val LOG = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("table spec file is needed")
      System.exit(1)
    }

    val tableSpecFile = args(0)
    val tableConfig = ConfigFactory.parseFile(new File(tableSpecFile)).withFallback(DEFAULT_CONFIG)

    val table = tableConfig.getString("table")
    val serde = tableConfig.getString("serde")
    val fields = tableConfig.getConfig("fields").root().unwrapped().map { kv =>
      val fieldName = kv._1
      val fieldType = kv._2.toString
      (fieldName, fieldType)
    }.toMap

    var database: String = null
    var serdeClass: String = null
    var inputFormatClass: String = null
    var outputFormatClass: String = null
    var isExternal: Boolean = false
    var tblproperties: Map[String, String] = null

    serde match {
      case "orc" =>
        database = "custom"
        serdeClass = ORC_SERDE
        inputFormatClass = ORC_INPUTFORMAT
        outputFormatClass = ORC_OUTPUTFORMAT
        isExternal = false
        tblproperties = Map("orc.compress" -> "SNAPPY")
      case _ =>
        database = "default"
        serdeClass = serde
        inputFormatClass = SYMLINK_INPUTFORMAT
        outputFormatClass = HIVE_IGNOREKEY_OUTPUTFORMAT
        isExternal = true
        tblproperties = Map()
    }

    val location = s"/user/hive/warehouse/${database}.db/${table}"

    val prefixSql = s"create ${if (isExternal) "external" else ""} table $database.$table"
    val fieldSql = fields.map(field => s"`${field._1}` ${field._2}").mkString(", ")
    val partitionSql = "partitioned by(`logdate` string)"
    val serdeSql = s"row format serde '$serdeClass'"
    val inputOutputFormatSql = s"stored as inputformat '$inputFormatClass' outputformat '$outputFormatClass'"
    val locationSql = s"location '$location'"
    val tblpropertiesSql = if (!tblproperties.isEmpty) s"tblproperties(${tblproperties.map(kv => s"'${kv._1}'='${kv._2}'").mkString(", ")})" else ""

    val createTableSql = s"$prefixSql ($fieldSql) $partitionSql $serdeSql $inputOutputFormatSql $locationSql $tblpropertiesSql"

    LOG.info(s"create table sql: $createTableSql")

    val config = ConfigFactory.load()
    val settings = new SparkSettings(config, args)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"CreateTable-$database.$table").setMaster("local")

    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)
    sqlContext.sql(createTableSql)
  }
}
