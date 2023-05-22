---
layout: docs
title: Server API
permalink: arangodb/server
---

```scala mdoc:invisible
import scala.concurrent._
import cats.effect._
import org.typelevel.log4cats._
import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2._
import cats.effect.unsafe.implicits.global

// implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
// implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]

val (arango, close) = Arango[IO](ArangoConfiguration.load()).allocated.unsafeRunSync()
```

# Server API

The examples assume `arango` is a ̀`ArangoClient[IO]`.

* get version

```scala mdoc:nest:height=15
arango.server.version().unsafeRunSync()
```

* get engine

```scala mdoc:nest:height=15
arango.server.engine().unsafeRunSync()
```

```scala mdoc:invisible
close.unsafeRunSync()
```