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

implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]

val arango = Arango(ArangoConfiguration.load())
```

# Database API

The examples assume `arango` is a fs2 ̀`Resource[IO, Arango[IO]]`.

Property `db` or `client` is an instance of [`ArangoDatabase[F]`](/avokka/api/avokka/arangodb/ArangoDatabase.html) for the database configured in `ArangoConfiguration.database`  

* list collections

```scala mdoc:nest:height=15
arango.use { client =>
  client.db.collections()
}.unsafeRunSync()
```
