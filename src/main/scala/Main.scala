import com.tomtung.crala.douban.{DoubanCrala, MusicEntryCsvStorage}
import com.weiglewilczek.slf4s.Logging
import io.Source

object Main extends Logging {
  def tryFor[T](times: Int)(op: => T): T = {
    try {
      op
    }
    catch {
      case e: Throwable => logger.warn("Operation failed, will try later.", e)
      Thread.sleep(5000)
      if (times >= 1)
        tryFor(times - 1)(op)
      else
        throw new RuntimeException("Operation failed.", e)
    }
  }
  
  /**
   * Read IDs from a text file, extract corresponding entries, and save them to a csv
   */
  def main(args: Array[String]) {
    val storage = new MusicEntryCsvStorage("music_entires.csv", append = true)

    var successiveErrorCount = 0;
    for (id <- Source.fromFile("music_entry_ids.txt").getLines()) {
      try {
        storage.add(tryFor(5)(DoubanCrala.extractMusicEntry(id)))
        successiveErrorCount = 0
      }
      catch {
        case e: Throwable =>
          logger.error("Failed to extract entry, id=" + id, e)
          successiveErrorCount += 1
          if (successiveErrorCount >= 5){
            val ms = (1 << (successiveErrorCount - 5)) * 30000
            logger.info(successiveErrorCount + " successive errors. Now sleep for " + ms + "ms")
            Thread.sleep(ms)
          }
      }
    }
    storage.close()
  }
}
