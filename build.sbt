name := "crala-douban"

organization := "com.tomtung"

scalaSource in Compile <<= baseDirectory(_ / "src")

libraryDependencies ++= Seq(
		"net.htmlparser.jericho" % "jericho-html" % "3.2",
		"org.joda" % "joda-convert" % "1.2",
		"joda-time" % "joda-time" % "2.0"
)
