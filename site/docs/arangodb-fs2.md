---
layout: docs
title: Fs2
position: 6
permalink: arangodb/fs2
---

# ArangoDB in FS2

Implementation of ArangoDB with fs2

Arango client database is written with [fs2 I/O](https://fs2.io/#/io) and query streaming with [fs2 stream](https://fs2.io/#/)

## Installation

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb-fs2" % "@VERSION@"
```

## Usage

```scala mdoc:invisible
import scala.concurrent._
import cats.effect._

implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
```

Let's use cats effect `IO` and assume we have a `ContextShift[_]` and a `Timer[_]` in implicit scope (maybe from `IOApp`)

```scala
import cats.effect._

implicit def cs: ContextShift[IO] = ???
implicit def timer: Timer[IO] = ???
```

We also need to provide a [log4cats](https://typelevel.org/log4cats/) `Logger[_]`

```scala mdoc:invisible
import org.typelevel.log4cats._
implicit val logger: Logger[IO] = noop.NoOpLogger[IO]
```
```scala
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger

implicit def unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]
```

And an ArangoConfiguration (we could also have use Cats-effect module for PureConfig)

```scala mdoc:silent
import avokka.arangodb.ArangoConfiguration

val configuration = ArangoConfiguration.load()
```

Then we build a `Resource` to connect to ArangoDB

```scala mdoc:height=5
import avokka.arangodb.fs2._

val arango = Arango(configuration)
```

We `.use` the resource to obtain an instance of [`ArangoClient[IO]`](/avokka/api/avokka/arangodb/protocol/ArangoClient.html)

```scala mdoc:nest:height=15
arango.use { client =>
  client.server.version()
}.unsafeRunSync()
```

Example in a for comprehension

```scala mdoc:nest:height=10
import avokka.arangodb.types._

val r = arango.use { client =>
  for {
    info    <- client.system.info()
    _       <- IO { println(s"database '${client.system.name}' is system = ${info.body.isSystem}") }
    cCount  <- client.db.collection(CollectionName("countries")).documents.count() 
  } yield cCount.body.count 
}

r.unsafeRunSync()
```

### Query result streaming with FS2 streams

Call `.stream[T]` on a `ArangoQuery[F]` to transform it to a `fs2.Stream[F, T]`

```scala mdoc:nest:height=15
arango.use { client =>
  client.db
    .query("FOR c IN countries RETURN c.name")
    .batchSize(100)
    .stream[String]
    .compile
    .toVector
}.unsafeRunSync()
```

### IOApp example

```scala
import avokka.arangodb.ArangoConfiguration
import avokka.arangodb.fs2._
import cats.effect._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.module.catseffect.syntax._

object ArangoExample extends IOApp {
  implicit def unsafeLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = for {
    config <- Blocker[IO].use(ArangoConfiguration.at().loadF[IO, ArangoConfiguration])
    arango = Arango(config)
    _ <- arango.use { client =>
      for {
        version <- client.server.version()
        _ <- IO { println(version.body) }
      } yield ()
    }
  } yield ExitCode.Success
}
```