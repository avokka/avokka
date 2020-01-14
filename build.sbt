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
  "-Ypartial-unification"
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
      testSuite ++
//      circe ++
      arango.map(_ % Test) ++
      logback.map(_ % Test)
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    libraryDependencies ++=
      akka ++
      testSuite ++
//      arango ++
      logback.map(_ % Test) ++
      Seq("com.typesafe.akka" %% "akka-slf4j" % akkaVersion % Test),
)

lazy val arangodb = (project in file("arangodb"))
  .dependsOn(velocystream)
  .aggregate(velocypack, velocystream)
  .settings(
    name := "avokka-arangodb",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    libraryDependencies ++=
//      enumeratum ++
      newtype ++
      pureconfig ++
      Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value) 
//      arango
  )

lazy val root = (project in file("."))
  .aggregate(arangodb, velocypack, velocystream)

