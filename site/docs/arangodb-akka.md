---
layout: docs
title: Akka
position: 5
permalink: arangodb/akka
---

# ArangoDB in Akka

Implementation of ArangoDB with Akka

Arango client database is written with akka I/O TCP and query streaming with akka streams

## Installation

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb-akka" % "@VERSION@"
```
```scala mdoc:invisible
import scala.concurrent._
import scala.concurrent.duration._
// import ExecutionContext.Implicits.global
import akka.stream.scaladsl._
import akka.NotUsed
```

## Usage

implicit akka actor system

```scala mdoc:invisible
import akka.actor.ActorSystem
implicit val system: ActorSystem = ActorSystem("docs")
```
```scala
import akka.actor.ActorSystem
implicit val system: ActorSystem = ???
```

default configuration

```scala mdoc:silent
import avokka.arangodb.ArangoConfiguration
val configuration = ArangoConfiguration.load()
```

arangodb client

```scala mdoc:silent
import avokka.arangodb.akka._
val arango = Arango(configuration)
```

### server api

query server version

```scala mdoc
Await.result(arango.server.version(), 10.seconds)
```

### database api

query collections

```scala mdoc:height=20
Await.result(arango.db.collections(), 10.seconds)
```

### query result streaming

model of arangodb document with a generated velocypack decoder

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
system.terminate()
```
