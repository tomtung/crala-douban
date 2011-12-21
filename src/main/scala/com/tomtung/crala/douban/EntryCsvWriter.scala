package com.tomtung.crala.douban

import au.com.bytecode.opencsv.CSVWriter
import java.io.{File, FileWriter}

class EntryCsvWriter(filename: String, fields: EntryField.ValueSet, append: Boolean = false) {
  private val fieldsArray = fields.toArray

  private val writer =
    if (append && new File(filename).exists())
      new CSVWriter(new FileWriter(filename, append))
    else {
      val w = new CSVWriter(new FileWriter(filename, append))
      w.writeNext(fieldsArray.map(_.toString))
      w
    }

  def writer(entry: Map[EntryField.Value, Any]) {
    writer.writeNext(fieldsArray.map(
      entry.get(_) match {
        case Some(l: List[Any]) => l.foldLeft("")((l, r) => l + ", " + r).substring(2)
        case Some(x) => x.toString
        case None => ""
      }))
    writer.flush()
  }

  def close() {
    writer.close()
  }
}