---
layout: docs
title: Akka
position: 5
permalink: arangodb/akka
---

# ArangoDB in Akka

Implementation of ArangoDB with Akka

Arango client database is written with [akka I/O TCP](https://doc.akka.io/docs/akka/current/io-tcp.html) and query streaming with [akka streams](https://doc.akka.io/docs/akka/current/stream/index.html)

## Installation

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb-akka" % "@VERSION@"
```

## Usage
```scala mdoc:invisible
import scala.concurrent._
import scala.concurrent.duration._
import akka.stream.scaladsl._
import akka.NotUsed
```

An implicit akka actor system

```scala mdoc:invisible
import akka.actor.ActorSystem
implicit val actorSystem: ActorSystem = ActorSystem("docs")
```
```scala
import akka.actor.ActorSystem
implicit def actorSystem: ActorSystem = ???
```

and an arango configuration

```scala mdoc:silent
import avokka.arangodb.ArangoConfiguration
val configuration = ArangoConfiguration.load()
```

build an arangodb client

```scala mdoc:silent
import avokka.arangodb.akka._
val arango = Arango(configuration)
```

`arango` is an instance of [`ArangoClient[Future]`](/avokka/api/avokka/arangodb/protocol/ArangoClient.html)


### Query result streaming with akka streams

Model of arangodb document with a generated velocypack decoder :

```scala mdoc
import avokka.arangodb.types._
import avokka.velocypack._

case class Country(_key: DocumentKey, name: String)
object Country {
  implicit val decoder: VPackDecoder[Country] = VPackDecoder.gen
}
```

simple query

```scala mdoc:height=10
val countries: Source[Country, NotUsed] = arango.db.query(
  "FOR c IN countries RETURN c"
).stream[Country]

Await.result(countries.runWith(Sink.seq), 10.seconds)
```

bind vars as argument

```scala mdoc
val countriesArg: Source[Country, NotUsed] = arango.db.query(
  "FOR c IN countries FILTER CONTAINS(c.name, @name) RETURN c", VObject("name" -> "Fra".toVPack)
).stream[Country]

Await.result(countriesArg.runWith(Sink.head), 10.seconds)
```

with aql interpolator

```scala mdoc
import avokka.arangodb.aql._
val name = "Fra"
val countriesAql: Source[Country, NotUsed] = arango.db.query(
  aql"FOR c IN countries FILTER CONTAINS(c.name, $name) RETURN c"
).stream[Country]

Await.result(countriesAql.runWith(Sink.head), 10.seconds)
```

or bind

```scala mdoc
val countriesBind: Source[Country, NotUsed] = arango.db.query(
  aql"FOR c IN countries FILTER CONTAINS(c.name, @name) RETURN c".bind("name", name)
).stream[Country]

Await.result(countriesBind.runWith(Sink.head), 10.seconds)
```

```scala mdoc:invisible
actorSystem.terminate()
```
