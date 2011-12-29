package com.tomtung.crala.douban

import java.net.URL
import concurrent.Lock
import java.lang.Thread
import util.Random
import com.weiglewilczek.slf4s.Logging
import net.htmlparser.jericho.Source
import java.io.FileNotFoundException
import scala.actors.Futures.future

class NaiveWebPageLoader(val timeout: Long) extends Logging {
  private val lock: Lock = new Lock()
  private val rand: Random = new Random()
  private val maxRetryCount = 5
  private val timeOut = 60000
  private val exponentialBackOffThreshold = 5
  private var successiveFailureCount = 0

  private def tryFor[T](times: Int)(op: => T): T = {
    try {
      op
    }
    catch {
      case e: FileNotFoundException =>
        throw e
      case e: Throwable =>
        if (times >= 1) {
          logger.warn("Operation failed, will try later.", e)
          Thread.sleep(5000)
          tryFor(times - 1)(op)
        }
        else throw e
    }
  }

  def loadSource(url: URL): Source = {
    lock.acquire()
    try {
      tryFor(maxRetryCount) {
        val source = {
          val connection = url.openConnection()
          connection.setConnectTimeout(timeOut)
          connection.setReadTimeout(timeOut)
          new Source(connection)
        }
        source.fullSequentialParse()

        successiveFailureCount = 0
        source
      }
    }
    catch {
      case e: FileNotFoundException =>
        logger.error("Faild to read from " + url, e)
        throw e
      case e: Throwable =>
        logger.error("Faild to read from " + url, e)
        successiveFailureCount += 1
        if (successiveFailureCount > exponentialBackOffThreshold) {
          val ms = (1 << (successiveFailureCount - 5)) * 30000
          logger.info(successiveFailureCount + " successive failures. Now sleep for " + ms + "ms")
          Thread.sleep(ms)
        }
        throw e
    }
    finally {
      future {
        Thread.sleep(timeout + rand.nextInt(500))
        lock.release()
      }
    }
  }
}

object NaiveWebPageLoader extends NaiveWebPageLoader(1500)