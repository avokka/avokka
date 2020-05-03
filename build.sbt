import Dependencies._

val scala212Version = "2.12.11"

ThisBuild / organization := "avokka"
ThisBuild / bintrayOrganization := Some("avokka")
ThisBuild / scalaVersion := scala212Version

ThisBuild / scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ypartial-unification"
)

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

lazy val velocypack = (project in file("velocypack"))
  .settings(
    name := "avokka-velocypack",
    description := "velocypack codec (scodec, shapeless, cats)",
    libraryDependencies ++= Seq(
      cats,
      shapeless,
    ) ++
      scodec ++
      testSuite :+ arango % Test
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    description := "velocystream client (akka IO)",
    libraryDependencies ++=
      akka ++
      testSuite
)

lazy val arangodb = (project in file("arangodb"))
  .dependsOn(velocystream)
  .aggregate(velocypack, velocystream)
  .settings(
    name := "avokka-arangodb",
    description := "ArangoDB client",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    libraryDependencies ++= Seq(
//      enumeratum,
      newtype,
      pureconfig,
      logging,
    ) ++ testSuite ++ akkaTestKit ++ dockerTest
  )

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocystream, arangodb)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
