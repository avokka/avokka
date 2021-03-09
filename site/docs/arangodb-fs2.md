---
layout: docs
title: Fs2
position: 6
permalink: arangodb/fs2
---

# ArangoDB in FS2

Implementation of ArangoDB with fs2

Arango client database and query streaming is written with fs2 io

## Installation

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb-fs2" % "@VERSION@"
```

## Usage

```scala mdoc:height=10
import avokka.arangodb.ArangoConfiguration

import scala.concurrent._
import cats.effect._
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import org.typelevel.log4cats._

implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]

val arango = Arango(ArangoConfiguration.load())

val r = arango.use { client =>
  for {
    version <- client.server.version()
    info    <- client.db.info()
    _       <- IO { println(s"database '${client.db.name}' is system = ${info.body.result.isSystem}") }
    cCount  <- client.db.collection(CollectionName("countries")).count() 
  } yield (version.body.version, cCount.body.count) 
}

val result = r.unsafeRunSync()

```