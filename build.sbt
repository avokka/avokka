import Dependencies._

val scala212Version = "2.12.13"
val scala213Version = "2.13.4"

ThisBuild / organization := "avokka"
ThisBuild / bintrayOrganization := Some("avokka")
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version)
ThisBuild / scalaVersion := scala213Version

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/avokka"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/avokka/avokka"),
    "scm:git:https://github.com/avokka/avokka.git",
    "scm:git:git@github.com:avokka/avokka.git",
  )
)
ThisBuild / developers := List(
  Developer(id="bicou", name="Benjamin VIELLARD", email="bicou@bicou.com", url = url("http://bicou.com/"))
)
ThisBuild / releasePublishArtifactsAction := PgpKeys.publishSigned.value

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
    description := "velocystream client (akka IO)",
    libraryDependencies ++=
      compatDeps ++
      akka ++
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
    description := "ArangoDB client",
    libraryDependencies ++= compatDeps ++ Seq(
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
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val avokkafs2 = (project in file("fs2"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-fs2",
    description := "ArangoDB with fs2",
    libraryDependencies ++=
      compatDeps ++ Seq(
        log4cats,
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
    libraryDependencies ++= Seq(
      arango,
      logback
    ),
    scalacOptions -= "-Xfatal-warnings"
  ).enablePlugins(JmhPlugin)

lazy val docs = (project in file("docs"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-docs",
    publishArtifact := false,
    skip in publish := true,
    scalacOptions -= "-Xfatal-warnings",
    mdocIn := (baseDirectory.value) / "src" / "main" / "paradox",
    Compile / paradox / sourceDirectory := mdocOut.value,
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
    mdocExtraArguments := Seq("--no-link-hygiene"), // paradox handles this
    git.remoteRepo := "git@github.com:avokka/avokka.git",
    ghpagesNoJekyll := true,
    Compile / paradoxMaterialTheme ~= {
      _.withColor("green", "green")
        .withRepository(uri("https://github.com/avokka/avokka"))
    }
  ).enablePlugins(ParadoxPlugin, ParadoxSitePlugin, ParadoxMaterialThemePlugin, MdocPlugin, GhpagesPlugin)

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocystream, arangodbTypes, arangodb, avokkafs2)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
