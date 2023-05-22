---
layout: docs
title: Collection API
permalink: arangodb/collection
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

arango.db.collection(CollectionName("temp")).drop().attempt.unsafeRunSync()
```

# Collection API

The examples assume `arango` is a ̀`ArangoClient[IO]`.

* list collections

```scala mdoc:height=15
arango.db.collections().unsafeRunSync()
```

* create collection

```scala mdoc:height=20
arango.db.collection(CollectionName("temp")).create().unsafeRunSync()
```

* delete collection

```scala mdoc:height=20
arango.db.collection(CollectionName("temp")).drop().unsafeRunSync()
```

* get info

```scala mdoc:height=20
arango.db.collection(CollectionName("countries")).info().unsafeRunSync()
```

```scala mdoc:invisible
close.unsafeRunSync()
```