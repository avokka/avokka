---
layout: home
title: Home
position: 1
---

# Introduction

Avokka provides building blocks to connect to [ArangoDB](https://github.com/arangodb/arangodb) database with [velocypack](https://github.com/arangodb/velocypack) serialization and [velocystream](https://github.com/arangodb/velocystream) protocol.

Compiled for scala 2.12 and scala 2.13

## Packages

* ### avokka-velocypack

Velocypack encoders and decoders built with scodec, shapeless and cats.

[Documentation]({% link velocypack.md %})

* ### avokka-velocystream

Models for the velocystream protocol

* ### avokka-arangodb

ArangoDB API core in tagless final style

[Documentation]({% link arangodb.md %})

* ### avokka-arangodb-akka

ArangoDB client implementation with akka I/O TCP and support for akka stream query result

[Documentation]({% link arangodb-akka.md %})

* ### avokka-arangodb-fs2

ArangoDB client implementation with fs2 I/O TCP and support for fs2.Stream query result

[Documentation]({% link arangodb-fs2.md %})
