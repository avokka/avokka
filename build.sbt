import Dependencies._

val scala212Version = "2.12.13"
val scala213Version = "2.13.4"

ThisBuild / organization := "com.bicou"
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version)
ThisBuild / scalaVersion := scala213Version

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/avokka"))
ThisBuild / developers := List(
  Developer(id="bicou", name="Benjamin VIELLARD", email="bicou@bicou.com", url = url("http://bicou.com/"))
)

ThisBuild / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 13)) => Seq(
    "-Ymacro-annotations"
  )
  case _ => Seq.empty
})

lazy val velocypack = (project in file("velocypack"))
  .settings(
    name := "avokka-velocypack",
    description := "velocypack codec (scodec, shapeless, cats)",
    libraryDependencies ++= compatDeps ++ Seq(
      cats,
      shapeless,
      magnolia,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    ) ++
      scodec ++
      testSuite :+ arango % Test,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    logBuffered in Test := false,
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    description := "velocystream models",
    libraryDependencies ++=
      compatDeps ++
      testSuite,
    logBuffered in Test := false,
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodbTypes = (project in file("arangodb-types"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-arangodb-types",
    description := "ArangoDB model types",
    libraryDependencies ++= Seq(
      newtype,
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
        case _ => Nil
      }),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodb = (project in file("arangodb"))
  .dependsOn(velocystream, arangodbTypes)
  .aggregate(velocypack, velocystream)
  .settings(
    name := "avokka-arangodb",
    description := "ArangoDB core",
    libraryDependencies ++= compatDeps ++ Seq(
      //      enumeratum,
      pureconfig,
      log4cats,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    ) ++ testSuite ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
        case _ => Nil
      }),
    logBuffered in Test := false,
    parallelExecution in Test := false,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodbAkka = (project in file("arangodb-akka"))
  .dependsOn(arangodb)
  .aggregate(velocypack, velocystream)
  .settings(
    name := "avokka-arangodb-akka",
    description := "ArangoDB client (akka)",
    libraryDependencies ++= compatDeps ++ akka ++ Seq(
//      enumeratum,
      pureconfig,
      logging,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    ) ++ testSuite ++ akkaTestKit ++ dockerTest ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
        case _ => Nil
      }),
    logBuffered in Test := false,
    parallelExecution in Test := false,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val avokkafs2 = (project in file("fs2"))
  .dependsOn(velocypack, velocystream)
  .settings(
    name := "avokka-fs2",
    description := "ArangoDB with fs2",
    skip in publish := true,
    libraryDependencies ++=
      compatDeps ++ Seq(
        log4catsSlf,
        scodecStream,
        catsRetry,
        catsEffect,
        fs2,
        fs2IO,
        pureconfig,
        pureconfigF % Test,
        logback % Test,
        scalaTest % Test,
        "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.0" % Test,
      ),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val bench = (project in file("bench"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-bench",
    skip in publish := true,
    libraryDependencies ++= Seq(
      arango,
      logback
    ),
    scalacOptions -= "-Xfatal-warnings"
  ).enablePlugins(JmhPlugin)

lazy val site = (project in file("site"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-site",
    publishArtifact := false,
    skip in publish := true,
    version := version.value.takeWhile(_ != '+'),
    scalacOptions -= "-Xfatal-warnings",
    mdocExtraArguments := Seq("--no-link-hygiene"),
    mdocVariables := Map(
     "VERSION" -> version.value
    ),
    micrositeName := "Avokka",
    micrositeDescription := "ArangoDB in pure scala",
    micrositeAuthor := "Benjamin Viellard",
    micrositeGithubOwner := "avokka",
    micrositeGithubRepo := "avokka",
//    micrositeUrl := "https://avokka.github.io/avokka",
    micrositeBaseUrl := "/avokka",
    micrositePushSiteWith := GHPagesPlugin,
    micrositeGitterChannel := false,
    micrositeHighlightTheme := "github",
    // micrositeHighlightLanguages ++= Seq("sbt"),
    micrositePalette ++= Map(
      "brand-primary" -> "#649d66",
      "brand-secondary" -> "#06623b",
    ),
    micrositeSearchEnabled := false,
    // micrositeDocumentationUrl := "documentation",
    micrositeTheme := "light",
    // micrositeDocumentationUrl := "docs",
  ).enablePlugins(MicrositesPlugin)

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocystream, arangodbTypes, arangodb, arangodbAkka, avokkafs2)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
