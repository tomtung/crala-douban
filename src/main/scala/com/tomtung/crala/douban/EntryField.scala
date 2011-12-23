package com.tomtung.crala.douban


object EntryField extends Enumeration {
  // Entry fields shared by music, book and movie
  val ID, Title, RatingCount, AverageRating, Tags, Recommendations, DidCount, DoingCount, WishCount, ReleaseDate = Value
  val generalFields = ValueSet.empty + ID + Title + RatingCount + AverageRating + Tags + Recommendations + DidCount + DoingCount + WishCount

  // Entry fields specific to music
  val Artists, ISBN = Value
  val musicFields = generalFields + Artists + ISBN
  
  // Entry fields specific to movies
  val Director, Screenwriter, Cast, Type, Region, Language, IMDb = Value
  val movieFields = generalFields + Director + Screenwriter + Cast + Type + Region + Language + IMDb
}
