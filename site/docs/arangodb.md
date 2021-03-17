---
layout: docs
title: ArangoDB
position: 4
permalink: arangodb
---

# ArangoDB

Package avokka-arangodb contains:

- core models of the arangodb protocol 
- arangodb dsl build with generic effect F[_] with constraints of cats
- [an AQL custom string interpolator]({% link arangodb-aql.md %})

You need to depends on the akka or the fs2 module to use the client.

### Configuration

An instance of [`ArangoConfiguration`](/avokka/api/avokka/arangodb/ArangoConfiguration.html) is needed to get an arangodb client.

A typesafe config reference is included :

```hocon
avokka {
  host = "localhost"
  port = 8529
  username = ""
  password = ""
  database = "_system"
  chunk-length = 30000
  read-buffer-size = 256000
  connect-timeout = "10s"
  reply-timeout = "30s"
}
```

```scala
import avokka.arangodb.ArangoConfiguration

// load configuration from defaults
val configuration = ArangoConfiguration.load()
```

### Implementation

ArangoDB client can use akka or fs2 implementation :

- [Akka]({% link arangodb-akka.md %})
- [FS2]({% link arangodb-fs2.md %})

Refer to the documentation of each module for usage.

### Usage

The common entry class is a [`ArangoClient[F]`](/avokka/api/avokka/arangodb/protocol/ArangoClient.html).

Property `client.db` is an instance of [`ArangoDatabase[F]`](/avokka/api/avokka/arangodb/ArangoDatabase.html) for the database configured in `ArangoConfiguration.database`

The majority of API calls return an instance of [`ArangoResponse[T]`](/avokka/api/avokka/arangodb/protocol/ArangoResponse.html), with a `header` property and a `body: T` property.

* [Server API]({% link arangodb-server.md %})
* [Database API]({% link arangodb-database.md %})
* [Collection API]({% link arangodb-collection.md %})
