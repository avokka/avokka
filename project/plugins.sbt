// scala compiler
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.17")

// publishing
addSbtPlugin("com.geirsson"     % "sbt-ci-release"  % "1.5.7")
addSbtPlugin("com.dwijnand"     % "sbt-dynver"      % "4.1.1")
addSbtPlugin("com.github.sbt"   % "sbt-pgp"         % "2.1.2")
addSbtPlugin("org.xerial.sbt"   % "sbt-sonatype"    % "3.9.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"         % "1.0.0")

/*
addSbtPlugin("org.foundweekends"  % "sbt-bintray" % "0.6.1")
addSbtPlugin("com.github.gseitz"  % "sbt-release" % "1.0.13")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"     % "2.1.1")
*/

// bench
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.0")

// docs
addSbtPlugin("com.47deg"    % "sbt-microsites" % "1.3.4")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc"     % "0.4.3")

/*
addSbtPlugin("com.lightbend.paradox"  % "sbt-paradox"                 % "0.9.1")
addSbtPlugin("io.github.jonas"        % "sbt-paradox-material-theme"  % "0.6.0")
addSbtPlugin("com.typesafe.sbt"       % "sbt-site"                    % "1.4.1")
addSbtPlugin("com.typesafe.sbt"       % "sbt-ghpages"                 % "0.6.3")
addSbtPlugin("org.scalameta"          % "sbt-mdoc"                    % "2.2.17")
*/

// tools
addSbtPlugin("io.chrisdavenport" %% "sbt-make-pom" % "0.0.2")

// coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.7"
