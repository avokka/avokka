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

implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]

val arango = Arango(ArangoConfiguration.load())
```

# Server API

The examples assume `arango` is a ̀`Resource[IO, Arango[IO]]`.

* get version

```scala mdoc:nest:height=15
arango.use { client =>
  client.server.version()
}.unsafeRunSync()
```

* get engine

```scala mdoc:nest:height=15
arango.use { client =>
  client.server.engine()
}.unsafeRunSync()
```

* list databases

```scala mdoc:nest:height=15
arango.use { client =>
  client.server.databases()
}.unsafeRunSync()
```
