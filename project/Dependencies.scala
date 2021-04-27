import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % "3.2.2.0"
  val scalaTestCatsEffect = "com.codecommit" %% "cats-effect-testing-scalatest" % "0.5.3"
//  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

//  val testSuite = Seq(scalaTest, scalaTestPlus, logback)

  val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.39.3"

  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.3"


  val cats = "org.typelevel" %% "cats-core" % "2.6.0"
//  val catsMtl = "org.typelevel" %% "cats-mtl" % "1.1.1"
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.5.0"
  val catsRetry = "com.github.cb372" %% "cats-retry" % "2.1.0"
  val fs2 = "co.fs2" %% "fs2-core" % "2.5.5"
  val fs2IO = "co.fs2" %% "fs2-io" % "2.5.5"

  val log4cats = "org.typelevel" %% "log4cats-core"    % "1.3.0"
  val log4catsSlf = "org.typelevel" %% "log4cats-slf4j" % "1.3.0"
  val log4catsNoop = "org.typelevel" %% "log4cats-noop" % "1.3.0"

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.4"

  val akkaVersion = "2.6.14"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion

  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val scodecBits = "org.scodec" %% "scodec-bits" % "1.1.26"
  val scodecCore = "org.scodec" %% "scodec-core" % "1.11.7"
  val scodecCats = "org.scodec" %% "scodec-cats" % "1.1.0-RC3"
  val scodecStream = "org.scodec" %% "scodec-stream" % "2.0.1"

  val arango = "com.arangodb" % "arangodb-java-driver" % "6.9.0"

  val enumeratumVersion = "1.6.1"
  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion

  val newtype = "io.estatico" %% "newtype" % "0.4.4"

  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.14.1"
  val pureconfigF = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.14.1"

  val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  val magnolia = "com.propensive" %% "magnolia" % "0.17.0"

  val circe = "io.circe" %% "circe-core" % "0.13.0"
  val circeLit = "io.circe" %% "circe-literal" % "0.13.0"
  val jawn = "org.typelevel" %% "jawn-parser" % "1.0.0"
}
