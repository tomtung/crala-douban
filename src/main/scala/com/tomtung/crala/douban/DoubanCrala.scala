package com.tomtung.crala.douban

import java.net.URL
import net.htmlparser.jericho.{HTMLElementName, Source}
import scala.collection.JavaConversions._
import com.weiglewilczek.slf4s.Logging
import java.io.InputStream
import collection.mutable.ListMap

class DoubanCrala(getInputStream: URL => InputStream) extends Logging {
  def extractMusicEntry(id: String): Map[String, Any] = {
    val entry = ListMap[String, Any]()
    def add(kv: (String, Any)) {
      logger.debug(kv._1 + ": " + kv._2)
      entry += kv
    }

    // ID
    add("ID" -> id)

    val source = new Source(getInputStream(new URL("http://music.douban.com/subject/" + id)))

    // Title
    add("Title" -> source.getFirstElement(HTMLElementName.H1).getTextExtractor.toString.trim)

    // Info
    val infoRenderer = source.getElementById("info").getRenderer
    infoRenderer.setIncludeHyperlinkURLs(false)
    infoRenderer.setMaxLineLength(Int.MaxValue)
    val info = infoRenderer.toString.split("\n").map(i => i.trim.split(": ")).map(i => (i(0).trim, i(1).trim)).toMap
    // Artists
    if (info.contains("表演者"))
      add("Artists" -> info("表演者").split("/").map(_.trim).toList)
    // Release date
    if (info.contains("发行时间"))
      add("Release Date" -> info("发行时间"))
    // ISBN
    if (info.contains("条型码"))
      add("ISBN" -> info("条型码"))

    // Rating count
    val ratingCountElem = source.getFirstElement("property", "v:votes", false)
    if (ratingCountElem != null)
      add("Rating Count" -> ratingCountElem.getTextExtractor.toString.toInt)

    // Average rating
    val avgRatingElem = source.getFirstElement("property", "v:average", false)
    if (avgRatingElem != null && !avgRatingElem.getTextExtractor.toString.isEmpty)
      add("Average Rating" -> avgRatingElem.getTextExtractor.toString.toDouble)

    // Tags (and the number of times they've been used)
    val tagsElem = source.getElementById("db-tags-section")
    if (tagsElem != null)
      add("Tags" ->
        tagsElem.getContent.getFirstElement(HTMLElementName.DIV)
          .getTextExtractor.toString.split("\\s+")
          .map("""(.+)\((\d+)\)""".r.findFirstMatchIn(_).get)
          .map(m => m.group(1)->m.group(2)))
    
    // Recommendations
    val recommendationsElem = source.getElementById("db-rec-section")
    if (recommendationsElem != null)
      add("Recommendations" ->
        recommendationsElem.getContent.getAllElements(HTMLElementName.DD)
          .map(_.getFirstElement(HTMLElementName.A).getAttributeValue("href"))
          .map("""subject/(.+)/""".r.findFirstMatchIn(_).get.group(1)).toList)

    // Did/doing/wish count
    val collectionsPage = new Source(getInputStream(new URL("http://music.douban.com/subject/" + id + "/collections")))

    add("Did Count" ->
      "\\d+".r.findFirstIn(
        collectionsPage.getElementById("collections_bar").getTextExtractor.toString).get.toInt)

    val doingCountElem = collectionsPage.getElementById("doings_bar")
    if (doingCountElem != null)
      add("Doing Count" ->
        "\\d+".r.findFirstIn(doingCountElem.getTextExtractor.toString).get.toInt)

    add("Wish Count" ->
      "\\d+".r.findFirstIn(
        collectionsPage.getElementById("wishes_bar").getTextExtractor.toString).get.toInt)

    logger.info("Extracted music entry, id=" + id)
    entry.toMap
  }
}

object DoubanCrala extends DoubanCrala(url => NaiveWebPageLoader.getInputStream(url))