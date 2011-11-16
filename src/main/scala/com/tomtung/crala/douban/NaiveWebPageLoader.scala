package com.tomtung.crala.douban

import java.net.URL
import concurrent.Lock
import java.lang.Thread
import java.io.InputStream
import util.Random

class NaiveWebPageLoader(val timeout: Long) {
  private val lock: Lock = new Lock()
  private val rand: Random = new Random()

  def delayedUnlock() {
    new Thread(new Runnable() {
      def run() {
        Thread.sleep(timeout + rand.nextInt(500))
        lock.release()
      }
    }).start()
  }

  def getInputStream(url: URL): InputStream = {
    lock.acquire()
    try {
      val connection = url.openConnection()
      connection.setConnectTimeout(60000)
      connection.setReadTimeout(60000)
      connection.getInputStream
    }
    finally {
      delayedUnlock()
    }
  }
}

object NaiveWebPageLoader extends NaiveWebPageLoader(1500)