---
layout: docs
title: ArangoDB
position: 4
permalink: arangodb
---

# ArangoDB

Package avokka-arangodb contains:

- core models of the arangodb protocol 
- arangodb dsl build with generic effect F[_]
- [an AQL custom string interpolator]({% link arangodb-aql.md %})

You need to depends on the akka or the fs2 module to use the client.

### Configuration

An instance of `ArangoConfiguration` is needed to get an arangodb client.

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
