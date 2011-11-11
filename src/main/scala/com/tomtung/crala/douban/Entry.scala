package com.tomtung.crala.douban

import org.joda.time.DateTime

/**
 * Contains information of a general Douban entry
 */
class Entry(val title: String = null,
            val releaseDate: String = null, /* Date format is too complex in Douban. Just extract string.*/
            val ratingCount: Int = 0, /* If less than 10, set to zero */
            val averageRating: Double = 0,
            val didCount: Int = 0,
            val doingCount: Int = 0,
            val wishCount: Int = 0,
            val tags: Map[String, Int] = Map(),
            val recommendations: List[String] = List() /* IDs of recommended items */) {
  val timestamp = DateTime.now
}