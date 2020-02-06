import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.1"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val testSuite = Seq(scalaTest, scalaCheck, logback).map(_ % Test)

  val cats = "org.typelevel" %% "cats-core" % "2.0.0"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  val akkaVersion = "2.5.26"
  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
  )

  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val scodec = Seq(
    "org.scodec" %% "scodec-bits" % "1.1.12",
    "org.scodec" %% "scodec-core" % "1.11.4",
    "org.scodec" %% "scodec-cats" % "1.0.0",
  )

  val arango = "com.arangodb" % "arangodb-java-driver" % "6.4.1"

  val enumeratumVersion = "1.5.13"
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion

  val newtype = "io.estatico" %% "newtype" % "0.4.3"

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.2"

}
