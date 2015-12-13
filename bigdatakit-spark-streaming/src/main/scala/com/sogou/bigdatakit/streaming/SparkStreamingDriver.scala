package com.sogou.bigdatakit.streaming

/**
 * Created by Tao Li on 12/13/15.
 */
trait SparkStreamingDriver {
  def start
  def stop
}
