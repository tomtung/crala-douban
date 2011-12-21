import com.tomtung.crala.douban._
import com.weiglewilczek.slf4s.Logger
import io.Source

val logger = Logger("CrawlMusicEntries")

// Read IDs from a text file, extract corresponding entries, and save them to a csv
val storage = new EntryCsvWriter("music_entires.csv", EntryField.musicFields)

for (id <- Source.fromFile("music_entry_ids.txt").getLines()) {
  try {
    storage.writer(DoubanCrala.extractMusicEntry(id))
  }
  catch {
    case e: Throwable =>
      logger.error("Failed to extract entry, id=" + id, e)
  }
}

storage.close()
