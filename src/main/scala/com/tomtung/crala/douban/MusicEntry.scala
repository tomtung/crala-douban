package com.tomtung.crala.douban


class MusicEntry(title: String = null,
                 releaseDate: String = null, /* Date format is too complex in Douban. Just extract string.*/
                 ratingCount: Int = 0, /* If less than 10, set to zero */
                 averageRating: Double = 0,
                 didCount: Int = 0,
                 doingCount: Int = 0,
                 wishCount: Int = 0,
                 tags: Map[String, Int] = Map(),
                 recommendations: List[String] = List(),
                 val artists: List[String] = List(),
                 val isbn: String = null)
  extends Entry(title, releaseDate, ratingCount, averageRating, didCount, doingCount, wishCount, tags, recommendations)