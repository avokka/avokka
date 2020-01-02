import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.1"

  lazy val testSuite = Seq(scalaTest, scalaCheck).map(_ % Test)

  val cats = Seq("org.typelevel" %% "cats-core" % "2.0.0")
  val shapeless = Seq("com.chuusai" %% "shapeless" % "2.3.3")

  val akkaVersion = "2.5.26"

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
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

  val arango = Seq("com.arangodb" % "arangodb-java-driver" % "6.4.1")

  val circeVersion = "0.12.3"

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  val enumeratumVersion = "1.5.13"
  val enumeratum = Seq("com.beachape" %% "enumeratum" % enumeratumVersion)

  val newtype = Seq("io.estatico" %% "newtype" % "0.4.3")

}
