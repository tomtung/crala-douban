package com.tomtung.crala.douban

import java.net.URL
import net.htmlparser.jericho.{HTMLElementName, Source}
import scala.collection.JavaConversions._
import com.weiglewilczek.slf4s.Logging
import collection.mutable.ListMap
import java.lang.String

class DoubanCrala(loadSource: URL => Source) extends Logging {
  def extractMusicEntry(id: String, fields: EntryField.ValueSet = EntryField.musicFields): Map[EntryField.Value, Any] = {
    val entry = ListMap[EntryField.Value, Any]()
    def add(kv: (EntryField.Value, Any)) {
      logger.debug(kv._1 + ": " + kv._2)
      entry += kv
    }

    // ID
    if (fields.contains(EntryField.ID))
      add(EntryField.ID -> id)

    val source = loadSource(new URL("http://music.douban.com/subject/" + id))

    // Title
    if (fields.contains(EntryField.Title))
      add(EntryField.Title -> source.getFirstElement(HTMLElementName.H1).getTextExtractor.toString.trim)

    // Info
    val info = {
      val infoRenderer = source.getElementById("info").getRenderer
      infoRenderer.setIncludeHyperlinkURLs(false)
      infoRenderer.setIncludeAlternateText(false)
      infoRenderer.setMaxLineLength(Int.MaxValue)
      def toKVPair(s: String): (String, String) = {
        val a = s.split(": ")
        if (a.size == 2) a(0).trim -> a(1).trim
        else {
          logger.warn("Possible wrong entry information line(entry id=" + id + "): [" + s + "]")
          null
        }
      }
      infoRenderer.toString.split("\n").map(toKVPair).filter(_ != null).toMap
    }

    // Artists
    if (fields.contains(EntryField.Artists) && info.contains("表演者"))
      add(EntryField.Artists -> info("表演者").split("/").map(_.trim).toList)

    // Release date
    if (fields.contains(EntryField.ReleaseDate) && info.contains("发行时间"))
      add(EntryField.ReleaseDate -> info("发行时间"))

    // ISBN
    if (fields.contains(EntryField.ISBN) && info.contains("条型码"))
      add(EntryField.ISBN -> info("条型码"))

    // Rating count
    if (fields.contains(EntryField.RatingCount)) {
      val ratingCountElem = source.getFirstElement("property", "v:votes", false)
      if (ratingCountElem != null)
        add(EntryField.RatingCount -> ratingCountElem.getTextExtractor.toString.toInt)
    }

    // Average rating
    if (fields.contains(EntryField.AverageRating)) {
      val avgRatingElem = source.getFirstElement("property", "v:average", false)
      if (avgRatingElem != null)
        add(EntryField.AverageRating -> avgRatingElem.getTextExtractor.toString.toDouble)
    }

    // Tags (and the number of times they've been used)
    if (fields.contains(EntryField.Tags)) {
      val tagsElem = source.getElementById("db-tags-section")
      if (tagsElem != null)
        add(EntryField.Tags -> tagsElem.getContent.getFirstElement(HTMLElementName.DIV)
          .getTextExtractor.toString.split("\\s+")
          .map("""(.+)\((\d+)\)""".r.findFirstMatchIn(_).get)
          .map(m => m.group(1) -> m.group(2)).toList)
    }

    // Recommendations
    if (fields.contains(EntryField.Recommendations)) {
      val recommendationsElem = source.getElementById("db-rec-section")
      if (recommendationsElem != null)
        add(EntryField.Recommendations -> recommendationsElem.getContent.getAllElements(HTMLElementName.DD)
          .map(_.getFirstElement(HTMLElementName.A).getAttributeValue("href"))
          .map("""subject/(.+)/""".r.findFirstMatchIn(_).get.group(1)).toList)
    }

    // Did/doing/wish count
    if (fields.contains(EntryField.DidCount) || fields.contains(EntryField.DoingCount) || fields.contains(EntryField.WishCount)) {
      val collectionsPage = loadSource(new URL("http://music.douban.com/subject/" + id + "/collections"))

      if (fields.contains(EntryField.DidCount))
        add(EntryField.DidCount -> "\\d+".r.findFirstIn(collectionsPage.getElementById("collections_bar").getTextExtractor.toString).get.toInt)

      if (fields.contains(EntryField.DoingCount)) {
        val doingCountElem = collectionsPage.getElementById("doings_bar")
        if (doingCountElem != null)
          add(EntryField.DoingCount -> "\\d+".r.findFirstIn(doingCountElem.getTextExtractor.toString).get.toInt)
      }

      if (fields.contains(EntryField.DoingCount))
        add(EntryField.WishCount -> "\\d+".r.findFirstIn(
          collectionsPage.getElementById("wishes_bar").getTextExtractor.toString).get.toInt)
    }
    logger.info("Extracted music entry, id=" + id)
    entry.toMap
  }
}

object DoubanCrala extends DoubanCrala(url => NaiveWebPageLoader.loadSource(url))