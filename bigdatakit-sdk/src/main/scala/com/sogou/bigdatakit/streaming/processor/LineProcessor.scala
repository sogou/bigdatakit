package com.sogou.bigdatakit.streaming.processor

/**
 * Created by Tao Li on 2015/8/28.
 */
trait LineProcessor extends java.io.Serializable {
  def init()

  def process(message: String)

  def close()
}
