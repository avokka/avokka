import Dependencies._

val scala212Version = "2.12.15"
val scala213Version = "2.13.6"

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
    Test / logBuffered := false,
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

lazy val velocypackCirce = (project in file("velocypack-circe"))
  .dependsOn(velocypack)
  .settings(
    name := "avokka-velocypack-circe",
    description := "velocypack encoders for circe json",
    libraryDependencies ++= Seq(
      circe
    ) ++ Seq(
      circeLit, jawn,
      scalaTest,
//      scalaTestPlus,
      logback,
    ).map(_ % Test),
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
    Test / logBuffered := false,
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
    Test / logBuffered := false,
    Test / parallelExecution := false,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings",
    Compile / sourceGenerators += Def.task {
      import com.github.tototoshi.csv._

      val reader = CSVReader.open((Compile / baseDirectory).value / "errors.dat")
      val errors = reader.all().map {
        case name :: code :: _ :: description :: Nil => s"""/** $description */
           |val ${name.stripPrefix("ERROR_")}: Long = ${code}L
           |""".stripMargin
        case _ => ""
      }.mkString
      val contents =
        s"""package avokka.arangodb.protocol
          |
          |object ArangoErrorNum {
          |$errors
          |}
          |""".stripMargin

      val file = (Compile / sourceManaged).value / "avokka" / "arangodb" / "protocol" / "ArangoErrorNum.scala"
      IO.write(file, contents)
      Seq(file)
    }.taskValue
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
    Test / logBuffered := false,
    Test / parallelExecution := false,
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodbFs2 = (project in file("arangodb-fs2"))
  .dependsOn(arangodb, test % "test->test", velocypackCirce % Test)
  .settings(
    name := "avokka-arangodb-fs2",
    description := "ArangoDB client (fs2)",
    libraryDependencies ++= Seq(
      collectionCompat,
      scodecStream,
      catsRetry,
      catsEffect,
      fs2,
      fs2IO,
    ) ++ Seq(
      log4catsSlf,
      logback,
      scalaTest,
      scalaTestPlus,
      scalaTestCatsEffect,
      circeLit,
      jawn,
      semver
    ).map(_ % Test),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings"
  )

lazy val arangodbFs2Ce3 = (project in file("arangodb-fs2-ce3"))
  .dependsOn(arangodb, test % "test->test", velocypackCirce % Test)
  .settings(
    name := "avokka-arangodb-fs2-ce3",
    description := "ArangoDB client (fs2,ce3)",
    libraryDependencies ++= Seq(
      collectionCompat,
      scodecStream_3,
      catsRetry_3,
      catsEffect_3,
      fs2_3,
      fs2IO_3,
    ) ++ Seq(
      log4catsSlf_3,
      logback,
      scalaTest,
      scalaTestPlus,
      scalaTestCatsEffect_3,
      circeLit,
      jawn,
      semver
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
    publish / skip := true,
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
    publish / skip := true,
    version := version.value.takeWhile(_ != '+'),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions -= "-Xfatal-warnings",
    mdocExtraArguments := Seq("--no-link-hygiene"),
    mdocVariables := Map(
     "VERSION" -> version.value
    ),
    micrositeName := "Avokka",
    micrositeDescription := "ArangoDB client for scala",
    micrositeAuthor := "Benjamin Viellard",
//    micrositeUrl := "https://avokka.github.io/avokka",
    micrositeBaseUrl := "/avokka",
    micrositeGitterChannel := false,
    micrositeSearchEnabled := false,
    micrositeTheme := "light",
    micrositeHighlightTheme := "github",
    micrositePalette ++= Map(
      "brand-primary" -> "#649d66",
      "brand-secondary" -> "#06623b",
    ),
    micrositeGithubOwner := "avokka",
    micrositeGithubRepo := "avokka",
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
    micrositePushSiteWith := GitHub4s,
    micrositeDocumentationLabelDescription := "Scaladoc",
    micrositeDocumentationUrl := "api/avokka",
    ScalaUnidoc / siteSubdirName := "api",
    addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, ScalaUnidoc / siteSubdirName),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(velocypack, velocystream, arangodb, arangodbAkka, arangodbFs2)
  ).enablePlugins(MicrositesPlugin, ScalaUnidocPlugin)

lazy val avokka = (project in file("."))
  .aggregate(velocypack, velocypackEnumeratum, velocypackCirce, velocystream, arangodb, arangodbAkka, arangodbFs2, arangodbFs2Ce3)
  .settings(
    publishArtifact := false,
    publish / skip := true
  ).disablePlugins(MakePomPlugin)
