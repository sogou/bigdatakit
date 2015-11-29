package com.sogou.bigdatakit.common.thread

import java.util.concurrent.ThreadFactory

/**
 * Created by Tao Li on 2015/8/28.
 */
class DaemonThreadFactory(name: String) extends ThreadFactory {
  override def newThread(r: Runnable): Thread = {
    val thread = new Thread(r)
    thread.setName(name)
    thread.setDaemon(true)
    thread
  }
}