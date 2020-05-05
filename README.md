# avokka [![Build Status](https://travis-ci.org/avokka/avokka.svg?branch=master)](https://travis-ci.org/avokka/avokka)

ArangoDB client written in pure scala

## Packages

### [avokka-velocypack](velocypack)

[Velocypack](https://github.com/arangodb/velocypack) codec for scala built with scodec, shapeless and cats

### [avokka-velocystream](velocystream)

[Velocystream](https://github.com/arangodb/velocystream) client (VST only) built with akka IO

### [avokka-arangodb](arangodb)

[ArangoDB](https://github.com/arangodb/arangodb) client

## Installation

Packages are published to bintray under the [`avokka`](https://bintray.com/avokka) organization.

SBT configuration :

```sbt
// add avokka bintray repository to resolvers
resolvers += Resolver.bintrayRepo("avokka", "maven")

// add dependency to the arangodb client
libraryDependencies += "avokka" %% "avokka-arangodb" % "0.0.3"

// or the velocystream client only
libraryDependencies += "avokka" %% "avokka-velocystream" % "0.0.3"

// or just the velocypack codec library
libraryDependencies += "avokka" %% "avokka-velocystream" % "0.0.3"
```
