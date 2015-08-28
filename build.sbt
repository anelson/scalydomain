lazy val commonSettings = Seq(
	organization := "org.apocryph",
  version := "1.0",
  scalaVersion := "2.11.7",

	resolvers += Resolver.sonatypeRepo("public"),

	fork in run := true,
	fork in test := true,
	fork in Test := true,
	baseDirectory in run := file("."),
	libraryDependencies ++= Seq(
  	"org.fusesource.leveldbjni" % "leveldbjni-osx" % "1.+",
	  "com.typesafe.akka" %% "akka-actor" % "2.3.+",
	  "com.github.scopt" %% "scopt" % "3.3.0",
	  "org.msgpack" %% "msgpack-scala" % "0.6.+",
	  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.+",
	  "ch.qos.logback" % "logback-core" % "1.1+",
	  "ch.qos.logback" % "logback-classic" % "1.1.+"
	),
	libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
	libraryDependencies += "org.scalactic" % "scalactic_2.11" % "2.2.+" % "test"
)

lazy val core = (project in file("core")).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-core"
  )

lazy val zoneimport = (project in file("zoneimport")).
	dependsOn(core).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-zoneimport"
	)

lazy val train = (project in file("train")).
	dependsOn(core).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-train"
	)

lazy val generate = (project in file("generate")).
	dependsOn(core).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-generate"
	)

lazy val root = (project in file(".")).
	aggregate(core, zoneimport, train, generate)
