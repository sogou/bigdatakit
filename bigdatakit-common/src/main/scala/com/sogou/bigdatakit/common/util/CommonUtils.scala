package com.sogou.bigdatakit.common.util

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.concurrent._
import java.util.{Map => JMap}

import com.sogou.bigdatakit.common.thread.CallableWithKillable

/**
 * Created by Tao Li on 2015/8/28.
 */
object CommonUtils {
  def getStackTraceStr(exception: Exception): String = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream
    exception.printStackTrace(new PrintStream(stream))
    stream.toString
  }

  def timeoutCall[V](callable: Callable[V], TIMEOUT_TIME: Long,
                     timeUnit: TimeUnit = TimeUnit.SECONDS): V = {
    val executor: ExecutorService = Executors.newFixedThreadPool(1)
    val future: FutureTask[V] = new FutureTask[V](callable)
    executor.execute(future)
    try {
      future.get(TIMEOUT_TIME, timeUnit)
    } catch {
      case e: Exception => throw e
    } finally {
      executor.shutdown
    }
  }

  def timeoutCallWithKill[V](callable: CallableWithKillable[V], TIMEOUT_TIME: Long,
                             timeUnit: TimeUnit = TimeUnit.SECONDS): V = {
    val executor: ExecutorService = Executors.newFixedThreadPool(1)
    val future: FutureTask[V] = new FutureTask[V](callable)
    executor.execute(future)
    try {
      future.get(TIMEOUT_TIME, timeUnit)
    } catch {
      case e: Exception => {
        callable.kill
        throw e
      }
    } finally {
      executor.shutdown
    }
  }

  def toStringJavaMap(charSeqMap: JMap[CharSequence, CharSequence]): JMap[String, String] = {
    import scala.collection.JavaConversions._
    for ((k: CharSequence, v: CharSequence) <- charSeqMap) yield (k.toString, v.toString)
  }
}
