import au.com.bytecode.opencsv.CSVWriter
import collection.mutable.HashSet
import com.tomtung.crala.douban._
import java.io.FileWriter

net.htmlparser.jericho.Config.LoggerProvider = net.htmlparser.jericho.LoggerProvider.DISABLED

val movies = new HashSet[String]()
val moviesWriter = new EntryCsvWriter("movie_entires.csv", EntryField.movieFields)
val ratingsWriter = new CSVWriter(new FileWriter("ratings.csv"))

io.Source.fromFile("users.txt").getLines().foreach(user => {
  val ratings = DoubanCrala.fetchWatchedMovieRatingsByUser(user)
  ratings.foreach(rating => {
    ratingsWriter.writeNext(rating match {
      case (i, Some(r), t) => Array(user, i.toString, r.toString, (t.getMillis / 1000).toString)
      case (i, None, t) => Array(user, i.toString, "", (t.getMillis / 1000).toString)
    })
    ratingsWriter.flush()

    if (!movies.contains(rating._1)) {
      try {
        moviesWriter.write(DoubanCrala.fetchMovieEntry(rating._1))
        movies += rating._1
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
      }
    }
  })
})

ratingsWriter.close()
moviesWriter.close()
