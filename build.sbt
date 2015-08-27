lazy val commonSettings = Seq(
	organization := "org.apocryph",
  version := "1.0",
  scalaVersion := "2.11.7",

	resolvers += Resolver.sonatypeRepo("public")
)

lazy val core = (project in file("core")).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-core"
	).
	settings (
		libraryDependencies ++= Seq(
	  	"org.iq80.leveldb" % "leveldb" % "0.7"
  	)
	)
lazy val zoneimport = (project in file("zoneimport")).
	dependsOn(core).
	settings(commonSettings: _*).
	settings(
	  name := "scalydomain-zoneimport"
	).
	settings (
		libraryDependencies ++= Seq(
	  	"com.typesafe.akka" %% "akka-actor" % "2.3.+",
	  	 "com.github.scopt" %% "scopt" % "3.3.0"
  	)
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
