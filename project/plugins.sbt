// scala compiler
// addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")

// publishing
addSbtPlugin("com.github.sbt"   % "sbt-ci-release"  % "1.5.12")
addSbtPlugin("com.dwijnand"     % "sbt-dynver"      % "4.1.1")
addSbtPlugin("com.github.sbt"   % "sbt-pgp"         % "2.2.1")
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"    % "3.9.21")
addSbtPlugin("com.github.sbt"   % "sbt-git"         % "2.0.1")

/*
addSbtPlugin("org.foundweekends"  % "sbt-bintray" % "0.6.1")
addSbtPlugin("com.github.gseitz"  % "sbt-release" % "1.0.13")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"     % "2.1.1")
*/

// bench
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.4")

// docs
addSbtPlugin("com.47deg"    % "sbt-microsites" % "1.4.3")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"     % "0.5.0")

/*
addSbtPlugin("com.lightbend.paradox"  % "sbt-paradox"                 % "0.9.1")
addSbtPlugin("io.github.jonas"        % "sbt-paradox-material-theme"  % "0.6.0")
addSbtPlugin("com.typesafe.sbt"       % "sbt-site"                    % "1.4.1")
addSbtPlugin("com.typesafe.sbt"       % "sbt-ghpages"                 % "0.6.3")
addSbtPlugin("org.scalameta"          % "sbt-mdoc"                    % "2.2.17")
*/

// tools
addSbtPlugin("io.chrisdavenport" %% "sbt-make-pom" % "0.0.3")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

// addSbtPlugin("ch.epfl.scala" % "sbt-scala3-migrate" % "0.5.1")
// sbt-dotty is not required since sbt 1.5.0-M1
// addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.3")
// addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
