import Dependencies._

val scala212Version = "2.12.10"

ThisBuild / organization := "com.bicou"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala212Version

ThisBuild / scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  //  "-Ypartial-unification"
)

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

lazy val velocypack = (project in file("velocypack"))
  .settings(
    name := "avokka-velocypack",
    libraryDependencies ++=
      cats ++
      shapeless ++
      scodec ++
      test ++
      arango.map(_ % Test) ++
      logback.map(_ % Test)
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    libraryDependencies ++=
      akka ++
      test ++
      logback.map(_ % Test)
  )

lazy val root = (project in file("."))
  .dependsOn(velocystream)
  .settings(
    name := "avokka",
  )

val test = Seq(scalaTest, scalaCheck).map(_ % Test)

// libraryDependencies += "org.scalamari" %% "velocypack4s-macros" % "0.0.1"

val cats = Seq("org.typelevel" %% "cats-core" % "2.0.0")
val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.3")

val akkaVersion = "2.5.26"

val akka = Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
)

val logback = Seq("ch.qos.logback" % "logback-classic" % "1.2.3")

val log = Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

val scodec = Seq(
  "org.scodec" %% "scodec-bits" % "1.1.12",
  "org.scodec" %% "scodec-core" % "1.11.4",
  "org.scodec" %% "scodec-cats" % "1.0.0",
)
// libraryDependencies += "org.typelevel" %% "spire" % "0.16.2"

val arango = Seq("com.arangodb" % "arangodb-java-driver" % "6.4.1")

val circeVersion = "0.12.3"

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)