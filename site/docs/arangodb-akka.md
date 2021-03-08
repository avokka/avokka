---
layout: docs
title: Akka
position: 5
permalink: arangodb/akka
---

# ArangoDB in Akka

Implementation of ArangoDB with Akka

Arango client database is written with akka I/O TCP and query streaming with akka streams

### Installation

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb-akka" % "@VERSION@"
```

### Usage

With an implicit actor system :

```scala
import akka.actor.ActorSystem
import avokka.arangodb.ArangoConfiguration

implicit val system: ActorSystem
val configuration = ArangoConfiguration.load()

import avokka.arangodb.akka._

// arangodb client
val arango = Arango(configuration)

// query server version
arango.server.version()
// : Future[ArangoResponse[Version]]

// database api
arango.db.collections()
// : Future[ArangoResponse[CollectionList]]


// model of arangodb document with a generated velocypack decoder
import avokka.arangodb.types._
import avokka.velocypack._
case class Country(_key: DocumentKey, name: String)
object Country {
  implicit val decoder: VPackDecoder[Country] = VPackDecoder.gen
}

// query streaming
import akka.NotUsed
import akka.stream.scaladsl.Source

// simple query
arango.db.query(
  "FOR c IN countries RETURN c", VObject.empty
).stream[Country]
// : Source[Country, NotUsed]

// explicit bind vars
arango.db.query(
  "FOR c IN countries FILTER CONTAINS(c.name, @name) RETURN c", VObject("name" -> "Fra".toVPack)
).stream[Country]
// : Source[Country, NotUsed]

// with aql interpolator
import avokka.arangodb.aql._
val name = "Fra"
arango.db.query(
  aql"FOR c IN countries FILTER CONTAINS(c.name, $name) RETURN c"
).stream[Country]
// : Source[Country, NotUsed]

// or bind
arango.db.query(
  aql"FOR c IN countries FILTER CONTAINS(c.name, @name) RETURN c".bind("name", name)
).stream[Country]
// : Source[Country, NotUsed]

```
