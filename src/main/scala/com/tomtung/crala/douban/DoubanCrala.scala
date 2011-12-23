package com.tomtung.crala.douban

import java.net.URL
import scala.collection.JavaConversions._
import com.weiglewilczek.slf4s.Logging
import java.lang.String
import org.joda.time.DateTime
import net.htmlparser.jericho.{Element, HTMLElementName, Source}
import collection.immutable.Map

class DoubanCrala(loadSource: URL => Source) extends Logging {

  type Entry = Map[EntryField.Value, Any]

  object EntryType extends Enumeration {
    val Music, Movie = Value
  }

  import EntryField._

  private def entryUrlToId: (String) => String = {
    """subject/(.+)/""".r.findFirstMatchIn(_).get.group(1)
  }

  def fetchMusicEntry(id: String, fields: EntryField.ValueSet = EntryField.musicFields) =
    fetchEntry(id, fields, EntryType.Music)

  def fetchMusicEntries(ids: Iterable[String], fields: EntryField.ValueSet = EntryField.musicFields) =
    fetchEntries(ids, fields, EntryType.Music)

  def fetchMovieEntry(id: String, fields: EntryField.ValueSet = EntryField.movieFields) =
    fetchEntry(id, fields, EntryType.Movie)

  def fetchMovieEntries(ids: Iterable[String], fields: EntryField.ValueSet = EntryField.movieFields) =
    fetchEntries(ids, fields, EntryType.Movie)

  private def infoFieldsExtractors(page: () => Source, entryType: EntryType.Value) = {
    lazy val info = {
      val renderer = page().getElementById("info").getRenderer
      renderer.setIncludeHyperlinkURLs(false)
      renderer.setIncludeAlternateText(false)
      renderer.setMaxLineLength(Int.MaxValue)
      renderer
    }.toString.split("\n")
      .map(l => l.split(": ").map(_.trim).toList match {
      case k :: v :: Nil => k -> v
      case _ => {
        logger.warn("Possibly wrong entry information line: \"" + l + "\"")
        null
      }
    }).filter(_ != null).toMap

    def infoValueExtractor(key: String) = () =>
      if (info.contains(key)) Some(info(key)) else None
    def infoListValueExtractor(key: String) = () =>
      if (info.contains(key)) Some(info(key).split("/").map(_.trim).toList)
      else None
    val releaseDateExtractor = entryType match {
      case EntryType.Movie =>
        infoValueExtractor("上映日期")
      case EntryType.Music =>
        infoValueExtractor("发行时间")
    }

    Map(
      Director -> infoListValueExtractor("导演"),
      Screenwriter -> infoListValueExtractor("编剧"),
      Cast -> infoListValueExtractor("主演"),
      ReleaseDate -> releaseDateExtractor,
      Language -> infoListValueExtractor("语言"),
      Region -> infoListValueExtractor("制片国家/地区"),
      Type -> infoListValueExtractor("类型"),
      IMDb -> infoValueExtractor("IMDb链接"),
      Artists -> infoListValueExtractor("表演者"),
      ISBN -> infoValueExtractor("条型码")
    )
  }

  private def mainPageFieldsExtractors(id: String, entryType: EntryType.Value) = {
    lazy val page = entryType match {
      case EntryType.Movie =>
        loadSource(new URL("http://movie.douban.com/subject/" + id))
      case EntryType.Music =>
        loadSource(new URL("http://music.douban.com/subject/" + id))
    }
    val titleExtractor = () =>
      Some(page.getFirstElement(HTMLElementName.H1).getFirstElement(HTMLElementName.SPAN)
        .getTextExtractor.toString.trim)
    val ratingCountExtractor = () =>
      page.getFirstElement("property", "v:votes", false) match {
        case null => None
        case e: Element => Some(e.getTextExtractor.toString.toInt)
      }
    val averageRatingExtractor = () =>
      page.getFirstElement("property", "v:average", false) match {
        case null => None
        case e: Element => Some(e.getTextExtractor.toString.toDouble)
      }
    val tagExtractor = () =>
      page.getElementById("db-tags-section") match {
        case null => None
        case e: Element => Some(e.getContent.getFirstElement(HTMLElementName.DIV)
          .getTextExtractor.toString.split("\\s+")
          .map("""(.+)\((\d+)\)""".r.findFirstMatchIn(_).get)
          .map(m => m.group(1) -> m.group(2)).toList)
      }
    val recommendationsExtractor = () =>
      page.getElementById("db-rec-section") match {
        case null => None
        case e: Element => Some(e.getContent.getAllElements(HTMLElementName.DD)
          .map(_.getFirstElement(HTMLElementName.A).getAttributeValue("href"))
          .map(entryUrlToId).toList)
      }

    Map(
      Title -> titleExtractor,
      RatingCount -> ratingCountExtractor,
      AverageRating -> averageRatingExtractor,
      Tags -> tagExtractor,
      Recommendations -> recommendationsExtractor
    ) ++ infoFieldsExtractors(() => page, entryType)
  }

  private def collectionsPageFieldsExtractors(id: String, entryType: EntryType.Value) = {
    lazy val collectionsPage = entryType match {
      case EntryType.Movie =>
        loadSource(new URL("http://movie.douban.com/subject/" + id + "/collections"))
      case EntryType.Music =>
        loadSource(new URL("http://music.douban.com/subject/" + id + "/collections"))
    }
    val didCountExtractor = () => {
      val text = collectionsPage.getElementById("collections_bar").getTextExtractor.toString
      Some("\\d+".r.findFirstIn(text).get.toInt)
    }
    val doingCountExtractor = () =>
      collectionsPage.getElementById("doings_bar") match {
        case null => None
        case e: Element => {
          val text = e.getTextExtractor.toString
          Some("\\d+".r.findFirstIn(text).get.toInt)
        }
      }
    val wishCountExtractor = () => {
      val text = collectionsPage.getElementById("wishes_bar").getTextExtractor.toString
      Some("\\d+".r.findFirstIn(text).get.toInt)
    }

    Map(
      DidCount -> didCountExtractor,
      DoingCount -> doingCountExtractor,
      WishCount -> wishCountExtractor
    )
  }

  def fetchEntry(id: String, fields: EntryField.ValueSet, entryType: EntryType.Value): Entry = {
    val fieldExtractor =
      Map(ID -> (() => Some(id))) ++
        mainPageFieldsExtractors(id, entryType) ++
        collectionsPageFieldsExtractors(id, entryType)

    val entry = fields.map(f => {
      val v = fieldExtractor.getOrElse(f, () => None).apply()
      (f, v)
    }).filter(_._2.isDefined).map(t => t._1 -> t._2.get).toMap
    logger.debug("Fetched " + entryType + " Entry, ID = " + id + ": " + entry)
    entry
  }

  def fetchEntries(ids: Iterable[String], fields: EntryField.ValueSet, entryType: EntryType.Value) =
    (for (id <- ids) yield try {
      fetchEntry(id, fields, entryType)
    }
    catch {
      case e: Throwable =>
        logger.error("Failed to extract entry, id=" + id, e)
        null
    }).filter(_ != null)

  def fetchWatchedMovieRatingsByUser(userId: String): Iterable[(String, Option[Int], DateTime)] = {
    val pages = {
      def pagesFrom(from: Int): Stream[Source] = try {
        logger.debug("Fetching watched movies ratings by " + userId + ", start from " + from)
        val url = new URL("http://movie.douban.com/people/" + userId + "/collect?mode=list&start=" + from)
        val page = loadSource(url)
        val numbers = page.getFirstElementByClass("subject-num").getTextExtractor.toString.split("""[^\d]+""").map(_.toInt).toArray
        val pageStart = numbers(0);
        val pageEnd = numbers(1);
        val end = numbers(2)
        if (pageStart > end) Stream.empty
        else if (pageEnd == end) Stream(page)
        else page #:: pagesFrom(pageEnd)
      }
      catch {
        case e: Throwable =>
          logger.error("Failed to load page (from " + from + ").", e)
          null
      }

      pagesFrom(0).filter(_ != null)
    }

    def pageToItems(page: Source): Iterable[(String, Option[Int], DateTime)] = {

      def elemToItem(e: Element): (String, Option[Int], DateTime) = try {
        val id = entryUrlToId(e.getFirstElement(HTMLElementName.A).getAttributeValue("href"))
        val rating = e.getFirstElement(HTMLElementName.SPAN) match {
          case null => None
          case re => Some("""rating(\d)-t""".r.findFirstMatchIn(re.getAttributeValue("class")).get.group(1).toInt)
        }
        val date = DateTime.parse(e.getFirstElementByClass("date").getTextExtractor.toString.trim())

        (id, rating, date)
      }
      catch {
        case e: Throwable =>
          logger.error("Failed to parse page.", e)
          null
      }

      page.getAllElementsByClass("item-show").map(elemToItem).filter(_ != null)
    }

    pages.flatMap(pageToItems)
  }
}

object DoubanCrala extends DoubanCrala(url => NaiveWebPageLoader.loadSource(url))