name := "crala-douban"

organization := "com.tomtung"

scalaSource in Compile <<= baseDirectory(_ / "src")

libraryDependencies ++= Seq(
		"net.htmlparser.jericho" % "jericho-html" % "3.2",
		"org.joda" % "joda-convert" % "1.2",
		"joda-time" % "joda-time" % "2.0",
		"net.sf.opencsv" % "opencsv" % "2.3",
		"com.weiglewilczek.slf4s" %% "slf4s" % "1.0.7",
		"org.slf4j" % "slf4j-log4j12" % "1.6.4"
)
