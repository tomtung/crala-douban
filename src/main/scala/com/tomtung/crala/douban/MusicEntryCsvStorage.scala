package com.tomtung.crala.douban

import au.com.bytecode.opencsv.CSVWriter
import java.io.{File, FileWriter}

class MusicEntryCsvStorage(filename: String, append: Boolean = false) {
  val features = List("ID", "Title", "Artists", "Release Date", "ISBN", "Rating Count", "Average Rating",
    "Tags", "Recommendations", "Did Count", "Doing Count", "Wish Count")
  private val writer =
    if (append && new File(filename).exists())
      new CSVWriter(new FileWriter(filename, append))
    else {
      val w = new CSVWriter(new FileWriter(filename, append))
      w.writeNext(features.toArray)
      w
    }

  def toValueStr(entry: Map[String, Any])(feature: String) = {
    if (!entry.contains(feature)) ""
    else entry(feature) match {
      case l: List[Any] =>
        l.foldLeft("")((l, r) => l + ", " + r).substring(2)
      case v => v.toString
    }
  }

  def add(entry: Map[String, Any]) {
    writer.writeNext(features.map(toValueStr(entry)).toArray)
    writer.flush()
  }

  def close() {
    writer.close()
  }
}