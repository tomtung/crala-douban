package com.tomtung.crala.douban

import java.net.URL
import net.htmlparser.jericho.{HTMLElementName, Source}
import scala.collection.JavaConversions._

object DoubanCrala {
  def extractMusicEntry(id: String) = {
    val source = new Source(new URL("http://music.douban.com/subject/" + id))

    // Title
    val title = source.getFirstElement(HTMLElementName.H1).getTextExtractor.toString.trim

    // Info
    val infoRenderer = source.getElementById("info").getRenderer
    infoRenderer.setIncludeHyperlinkURLs(false)
    val info = infoRenderer.toString.split("\n").map(i => i.trim.split(": ")).map(i => (i(0).trim, i(1).trim)).toMap
    // Artists
    val artists =
      if (info.contains("表演者")) info("表演者").split("/").map(_.trim).toList
      else List()
    // Release date
    val releaseDate =
      if (info.contains("发行时间")) info("发行时间") else ""
    // ISBN
    val isbn =
      if (info.contains("条型码")) info("条型码") else ""

    // Rating count
    val ratingCountElem = source.getFirstElement("property", "v:votes", false)
    val ratingCount =
      if (ratingCountElem == null) 0
      else ratingCountElem.getTextExtractor.toString.toInt

    // Average rating
    val avgRatingElem = source.getFirstElement("property", "v:average", false)
    val avgRating =
      if (avgRatingElem == null || avgRatingElem.getTextExtractor.toString.isEmpty) 0
      else avgRatingElem.getTextExtractor.toString.toDouble

    // Tags
    val tagsElem = source.getElementById("db-tags-section")
    val tags =
      if (tagsElem == null) Map[String, Int]()
      else
        tagsElem.getContent.getFirstElement(HTMLElementName.DIV)
          .getTextExtractor.toString.split("\\s+")
          .map(_.split("""[\(\)]""")).map(a => (a(0), a(1).toInt)).toMap

    // Recommendations
    val recommendationsElem = source.getElementById("db-rec-section")
    val recommendations =
      if (recommendationsElem == null) List()
      else
        recommendationsElem.getContent.getAllElements(HTMLElementName.DD)
          .map(_.getFirstElement(HTMLElementName.A).getAttributeValue("href"))
          .map("""subject/(\d+)""".r.findFirstMatchIn(_).get.group(1)).toList

    // Did/doing/wish count
    val collectionsPage = new Source(new URL("http://music.douban.com/subject/" + id + "/collections"))

    val didCount = "\\d+".r.findFirstIn(
      collectionsPage.getElementById("collections_bar").getTextExtractor.toString).get.toInt

    val doingCountElem = collectionsPage.getElementById("doings_bar")
    val doingCount =
      if (doingCountElem == null) 0
      else "\\d+".r.findFirstIn(doingCountElem.getTextExtractor.toString).get.toInt

    val wishCount = "\\d+".r.findFirstIn(
      collectionsPage.getElementById("wishes_bar").getTextExtractor.toString).get.toInt

    // The entry is ready
    new MusicEntry(title = title, releaseDate = releaseDate, ratingCount = ratingCount,
      averageRating = avgRating, didCount = didCount, doingCount = doingCount, wishCount = wishCount,
      tags = tags, recommendations = recommendations, artists = artists, isbn = isbn)
  }
}