import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.10",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "avokka",
    libraryDependencies ++= Seq(scalaTest, scalaCheck).map(_ % Test)
  )

libraryDependencies += "org.scalamari" %% "velocypack4s-macros" % "0.0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.26",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.26" % Test
)
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.26",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

libraryDependencies ++=  Seq(
  "org.scodec" %% "scodec-bits" % "1.1.12",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scodec" %% "scodec-cats" % "1.0.0",
)
libraryDependencies += "org.typelevel" %% "spire" % "0.16.2"

libraryDependencies += "com.arangodb" % "arangodb-java-driver" % "6.4.1"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)