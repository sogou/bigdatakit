package com.sogou.bigdatakit.etl.hbase

import com.sogou.bigdatakit.etl.RDDTransformer
import org.apache.avro.specific.SpecificRecordBase

/**
  * Created by Tao Li on 2016/3/30.
  */
trait HBaseCFAvroTransformer extends RDDTransformer[(String, Map[String, (SpecificRecordBase, Long)])]