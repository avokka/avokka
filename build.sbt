import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.10",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "avokka",
    libraryDependencies += scalaTest % Test
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
  "org.scodec" %% "scodec-core" % "1.11.4"
)
