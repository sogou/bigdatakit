package com.sogou.bigdatakit.hive

import java.io.File

import com.sogou.bigdatakit.spark.SparkSettings
import com.typesafe.config.ConfigFactory
import org.apache.avro.Schema
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.hive.HiveContext
import org.slf4j.LoggerFactory

/**
  * Created by Tao Li on 2016/1/4.
  */
object CreateTable {
  val DEFAULT_DATABASE = "custom"
  val ORC_INPUTFORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat"
  val ORC_OUTPUTFORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat"
  val ORC_SERDE = "org.apache.hadoop.hive.ql.io.orc.OrcSerde"

  private val LOG = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("table avsc file is needed")
      System.exit(1)
    }

    val avscFile = args(0)
    val schema = new Schema.Parser().parse(new File(avscFile))

    var database = schema.getNamespace
    val table = schema.getName
    var inputformat = schema.getProp("inputformat")
    var outputformat = schema.getProp("outputformat")
    var serde = schema.getProp("serde")
    var location = schema.getProp("location")

    if (database == null) database = DEFAULT_DATABASE
    if (inputformat == null) inputformat = ORC_INPUTFORMAT
    if (outputformat == null) outputformat = ORC_OUTPUTFORMAT
    if (serde == null) serde = ORC_SERDE
    if (location == null) location = s"/user/hive/warehouse/${database}.db/${table}"
    val external = database match {
      case "default" => "external"
      case _ => ""
    }

    import scala.collection.JavaConversions._

    val prefixSql = s"create $external table $database.$table"
    val fieldSql = schema.getFields().map(f =>
      s"`${f.name()}` ${f.schema().getType.toString.toLowerCase}").mkString(", ")
    val partitionSql = "partitioned by(`logdate` string)"
    val serdeSql = s"row format serde '$serde'"
    val inputOutputFormatSql = s"stored as inputformat '$inputformat' outputformat '$outputformat'"
    val locationSql = s"location '$location'"
    val propertySql = ""

    val createTableSql = s"$prefixSql ($fieldSql) $partitionSql $serdeSql $inputOutputFormatSql $locationSql $propertySql"

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
