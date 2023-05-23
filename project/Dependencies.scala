import sbt._

object Dependencies {

  /* scala */
  val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0"
  val kindProjector = "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % "0.3.1"

  /* types */
  val enumeratum = "com.beachape" %% "enumeratum" % "1.7.2"
  val newtype = "io.estatico" %% "newtype" % "0.4.4"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
  val shapeless3 = "org.typelevel" % "shapeless3-deriving_3" % "3.3.0"
  val magnolia = "com.softwaremill.magnolia1_2" %% "magnolia" % "1.1.3"
  val magnolia3 = "com.softwaremill.magnolia1_3" %% "magnolia" % "1.2.5"

  /* tests */
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16"
  val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0"
  // val scalaTestCatsEffect = "com.codecommit" %% "cats-effect-testing-scalatest" % "0.5.4"
  val scalaTestCatsEffect_3 = "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0"
  val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.15"

  /* logging */
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.12"
  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"

  /* cats */
  val cats = "org.typelevel" %% "cats-core" % "2.9.0"
  // val catsEffect = "org.typelevel" %% "cats-effect" % "2.5.5"
  // val catsRetry = "com.github.cb372" %% "cats-retry" % "2.1.1"
  // val log4cats = "org.typelevel" %% "log4cats-core"    % "1.3.1"
  // val log4catsSlf = "org.typelevel" %% "log4cats-slf4j" % "1.3.1"
  // val log4catsNoop = "org.typelevel" %% "log4cats-noop" % "1.3.1"
  // ce3
  val catsEffect_3 = "org.typelevel" %% "cats-effect" % "3.5.0"
  val catsRetry_3 = "com.github.cb372" %% "cats-retry" % "3.1.0"
  val log4cats_3 = "org.typelevel" %% "log4cats-core"    % "2.6.0"
  val log4catsSlf_3 = "org.typelevel" %% "log4cats-slf4j" % "2.6.0"
  val log4catsNoop_3 = "org.typelevel" %% "log4cats-noop" % "2.6.0"

  /* fs2 */
  // val fs2 = "co.fs2" %% "fs2-core" % "2.5.11"
  // val fs2IO = "co.fs2" %% "fs2-io" % "2.5.11"
  // ce3
  val fs2_3 = "co.fs2" %% "fs2-core" % "3.7.0"
  val fs2IO_3 = "co.fs2" %% "fs2-io" % "3.7.0"

  /* akka */
  val akkaVersion = "2.8.2"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  /* scodec */
  val scodecBits = "org.scodec" %% "scodec-bits" % "1.1.37"
  val scodecCore = "org.scodec" %% "scodec-core" % "1.11.10"
  val scodecCore2 = "org.scodec" %% "scodec-core" % "2.2.0"
  val scodecCats = "org.scodec" %% "scodec-cats" % "1.2.0"
  val scodecStream = "org.scodec" %% "scodec-stream" % "2.0.3"
  val scodecStream_3 = "org.scodec" %% "scodec-stream" % "3.0.2"

  /* config */
  val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.17.4"
  val pureconfig3 = "com.github.pureconfig" %% "pureconfig-core" % "0.17.4"
  val pureconfigF = "com.github.pureconfig" %% "pureconfig-cats-effect2" % "0.17.4"
  val pureconfigF_3 = "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.4"

  /* circe */
  val circe = "io.circe" %% "circe-core" % "0.14.5"
  val circeLit = "io.circe" %% "circe-literal" % "0.14.5"
  val jawn = "org.typelevel" %% "jawn-parser" % "1.4.0"

  /* utils */
  val semver = "io.kevinlee" %% "just-semver" % "0.6.0"

  /* official */
  val arango = "com.arangodb" % "velocypack" % "3.0.0"

}
