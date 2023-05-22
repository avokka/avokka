---
layout: docs
title: Database API
permalink: arangodb/database
---

```scala mdoc:invisible
import scala.concurrent._
import cats.effect._
import org.typelevel.log4cats._
import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2._
import avokka.arangodb.types._
import cats.effect.unsafe.implicits.global

// implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
// implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]

val (arango, close) = Arango[IO](ArangoConfiguration.load()).allocated.unsafeRunSync()

arango.database(DatabaseName("temp")).drop().attempt.unsafeRunSync()
```

# Database API

The examples assume `arango` is a ̀`ArangoClient[IO]`.

* list server databases

```scala mdoc:height=15
arango.server.databases().unsafeRunSync()
```

* create database

```scala mdoc:height=15
arango.database(DatabaseName("temp")).create().unsafeRunSync()
```

* delete database

```scala mdoc:height=15
arango.database(DatabaseName("temp")).drop().unsafeRunSync()
```

* get database information

```scala mdoc:height=15
arango.db.info().unsafeRunSync()
```


```scala mdoc:invisible
close.unsafeRunSync()
```