import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"
  val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % "3.1.1.1"
//  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val testSuite = Seq(scalaTest, scalaTestPlus, logback).map(_ % Test)

  val dockerTest = Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.37.0"
  ).map(_ % Test)

  val compatDeps = Seq(
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6"
  )

  val cats = "org.typelevel" %% "cats-core" % "2.1.1"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  val akkaVersion = "2.5.28"
  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  )

  val akkaTestKit = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  ).map(_ % Test)

  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val scodec = Seq(
    "org.scodec" %% "scodec-bits" % "1.1.14",
    "org.scodec" %% "scodec-core" % "1.11.7",
    "org.scodec" %% "scodec-cats" % "1.0.0",
  )

  val arango = "com.arangodb" % "arangodb-java-driver" % "6.4.1"

  val enumeratumVersion = "1.5.13"
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion

  val newtype = "io.estatico" %% "newtype" % "0.4.3"

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.3"

}
