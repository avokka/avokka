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
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.1"
  )

  val cats = "org.typelevel" %% "cats-core" % "2.3.1"
  val catsMtl = "org.typelevel" %% "cats-mtl" % "1.1.1"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.3.1"
  val catsRetry = "com.github.cb372" %% "cats-retry" % "2.1.0"
  val fs2 = "co.fs2" %% "fs2-core" % "2.5.0"
  val fs2IO = "co.fs2" %% "fs2-io" % "2.5.0"
  val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  val akkaVersion = "2.6.5"
  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  )

  val akkaTestKit = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion
  ).map(_ % Test)

  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val scodec = Seq(
    "org.scodec" %% "scodec-bits" % "1.1.23",
    "org.scodec" %% "scodec-core" % "1.11.7",
    "org.scodec" %% "scodec-cats" % "1.1.0-M4",
  )
  val scodecStream = "org.scodec" %% "scodec-stream" % "2.0.0"

  val arango = "com.arangodb" % "arangodb-java-driver" % "6.4.1"

  val enumeratumVersion = "1.6.1"
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion

  val newtype = "io.estatico" %% "newtype" % "0.4.4"

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.14.0"
  val pureconfigF = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.14.0"

  val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  val magnolia = "com.propensive" %% "magnolia" % "0.17.0"
}
