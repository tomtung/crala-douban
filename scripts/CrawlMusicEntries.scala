import com.tomtung.crala.douban._

// Read IDs from a text file, extract corresponding entries, and save them to a csv
val storage = new EntryCsvWriter("music_entires.csv", EntryField.musicFields)

var ids = io.Source.fromFile("music_entry_ids.txt").getLines().toIterable
DoubanCrala.fetchMusicEntries(ids).foreach(storage.write)

storage.close()
