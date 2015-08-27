lazy val commonSettings = Seq(
	organization := "org.apocryph",
  version := "1.0",
  scalaVersion := "2.11.7",

	resolvers += Resolver.sonatypeRepo("public"),

	fork in run := true,
	fork in test := true,
	baseDirectory in run := file("."),
	libraryDependencies ++= Seq(
  	"org.fusesource.leveldbjni" % "leveldbjni-osx" % "1.8",
	  "com.typesafe.akka" %% "akka-actor" % "2.3.+",
	  "com.github.scopt" %% "scopt" % "3.3.0",
	  "org.msgpack" %% "msgpack-scala" % "0.6.11"
	)
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
