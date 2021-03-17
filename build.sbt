import Dependencies._

val scala212Version = "2.12.12"
val scala213Version = "2.13.5"

ThisBuild / organization := "com.bicou"
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version)
ThisBuild / scalaVersion := scala213Version

ThisBuild / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 13)) => Seq(
    "-Ymacro-annotations"
  )
  case _ => Seq.empty
})

ThisBuild / javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/avokka"))
ThisBuild / developers := List(
  Developer(id="bicou", name="Benjamin VIELLARD", email="bicou@bicou.com", url = url("http://bicou.com/"))
)

lazy val velocypack = (project in file("velocypack"))
  .settings(
    name := "avokka-velocypack",
    description := "velocypack codec (scodec, shapeless, cats)",
    libraryDependencies ++= Seq(
      collectionCompat,
      cats,
      scodecCore,
      scodecBits,
      scodecCats,
      shapeless,
      magnolia,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    ) ++ Seq(
      arango,
      scalaTest,
      scalaTestPlus,
      logback,
    ).map(_ % Test),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    logBuffered in Test := false,
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val velocypackEnumeratum = (project in file("velocypack-enumeratum"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocypack-enumeratum",
    description := "velocypack support for enumeratum",
    libraryDependencies ++= Seq(
      enumeratum
    ),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val velocystream = (project in file("velocystream"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocystream",
    description := "velocystream models",
    libraryDependencies ++= Seq(
      collectionCompat
    ) ++ Seq(
      scalaTest,
      logback,
    ).map(_ % Test),
    logBuffered in Test := false,
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodb = (project in file("arangodb"))
  .dependsOn(velocystream, velocypackEnumeratum)
  .settings(
    name := "avokka-arangodb",
    description := "ArangoDB core",
    libraryDependencies ++= Seq(
      collectionCompat,
      newtype,
      pureconfig,
      log4cats,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
        case _ => Nil
      }) ++ Seq(
      scalaTest,
      scalaTestPlus,
      logback,
    ).map(_ % Test),
    logBuffered in Test := false,
    parallelExecution in Test := false,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodbAkka = (project in file("arangodb-akka"))
  .dependsOn(arangodb, test % "test->test")
  .settings(
    name := "avokka-arangodb-akka",
    description := "ArangoDB client (akka)",
    libraryDependencies ++= Seq(
      collectionCompat,
      akkaActor,
      akkaStream,
      logging,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    ) ++ Seq(
      scalaTest,
      akkaTestKit,
      logback,
    ).map(_ % Test) ++
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

lazy val arangodbFs2 = (project in file("arangodb-fs2"))
  .dependsOn(arangodb, test % "test->test")
  .settings(
    name := "avokka-arangodb-fs2",
    description := "ArangoDB client (fs2)",
    libraryDependencies ++= Seq(
      collectionCompat,
//      log4cats,
      scodecStream,
      catsRetry,
      catsEffect,
      fs2,
      fs2IO,
 //     pureconfig,
    ) ++ Seq(
      log4catsSlf,
      pureconfigF,
      logback,
      scalaTest,
      scalaTestPlus,
      scalaTestCatsEffect,
    ).map(_ % Test),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val test = (project in file("test"))
  .dependsOn(arangodb)
  .settings(
    name := "avokka-test",
    libraryDependencies ++= Seq(
      testContainers
    ),
    scalacOptions -= "-Xfatal-warnings"
  )


lazy val bench = (project in file("bench"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-bench",
    publishArtifact := false,
    skip in publish := true,
    libraryDependencies ++= Seq(
      arango,
      logback
    ),
    scalacOptions -= "-Xfatal-warnings"
  ).enablePlugins(JmhPlugin)

lazy val site = (project in file("site"))
  .dependsOn(arangodbAkka, arangodbFs2)
  .settings(
    libraryDependencies ++= Seq(
      log4catsNoop,
      log4catsSlf,
      logback,
      pureconfigF,
    ),
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
    micrositeDescription := "ArangoDB client in scala",
    micrositeAuthor := "Benjamin Viellard",
//    micrositeUrl := "https://avokka.github.io/avokka",
    micrositeBaseUrl := "/avokka",
    micrositeGitterChannel := false,
    micrositeSearchEnabled := false,
    micrositeTheme := "light",
    micrositeHighlightTheme := "github",
    // micrositeHighlightLanguages ++= Seq("sbt"),
    micrositePalette ++= Map(
      "brand-primary" -> "#649d66",
      "brand-secondary" -> "#06623b",
    ),
    micrositeGithubOwner := "avokka",
    micrositeGithubRepo := "avokka",
    micrositePushSiteWith      := GHPagesPlugin,
    micrositeDocumentationLabelDescription := "Scaladoc",
    micrositeDocumentationUrl := "api/avokka",
    siteSubdirName in ScalaUnidoc := "api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(velocypack, velocystream, arangodb, arangodbAkka, arangodbFs2)
  ).enablePlugins(MicrositesPlugin, ScalaUnidocPlugin)

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocypackEnumeratum, velocystream, arangodb, arangodbAkka, arangodbFs2)
  .settings(
    publishArtifact := false,
    skip in publish := true
  )
