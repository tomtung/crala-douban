package com.tomtung.crala.douban


object EntryField extends Enumeration {
  // Entry fields shared by music, book and movie
  val ID, Title, RatingCount, AverageRating, Tags, Recommendations, DidCount, DoingCount, WishCount = Value
  val generalFields = ValueSet.empty + ID + Title + RatingCount + AverageRating + Tags + Recommendations + DidCount + DoingCount + WishCount

  // Entry fields specific to music
  val Artists, ReleaseDate, ISBN = Value
  val musicFields = generalFields + Artists + ReleaseDate + ISBN
}

