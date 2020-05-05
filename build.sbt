import Dependencies._

val scala212Version = "2.12.11"
val scala213Version = "2.13.2"

ThisBuild / organization := "avokka"
ThisBuild / bintrayOrganization := Some("avokka")
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version)

ThisBuild / scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xlint"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => Seq(
    "-Ypartial-unification",
  )
  case _ => Seq(
    "-Ymacro-annotations"
  )
})

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

lazy val velocypack = (project in file("velocypack"))
  .settings(
    name := "avokka-velocypack",
    description := "velocypack codec (scodec, shapeless, cats)",
    libraryDependencies ++= compatDeps ++ Seq(
      cats,
      shapeless,
    ) ++
      scodec ++
      testSuite :+ arango % Test,
    logBuffered in Test := false
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    description := "velocystream client (akka IO)",
    libraryDependencies ++=
      compatDeps ++
      akka ++
      testSuite,
    logBuffered in Test := false
  )

lazy val arangodb = (project in file("arangodb"))
  .dependsOn(velocystream)
  .aggregate(velocypack, velocystream)
  .settings(
    name := "avokka-arangodb",
    description := "ArangoDB client",
    libraryDependencies ++= compatDeps ++ Seq(
//      enumeratum,
      newtype,
      pureconfig,
      logging,
    ) ++ testSuite ++ akkaTestKit ++ dockerTest ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
        )
      case _ =>
        // if scala 2.13.0-M4 or later, macro annotations merged into scala-reflect
        // https://github.com/scala/scala/pull/6606
        Nil
    }),
    logBuffered in Test := false,
    parallelExecution in Test := false
  )

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocystream, arangodb)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
