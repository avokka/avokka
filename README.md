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

SBT configuration :

```sbt
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb" % "0.0.5"

// or the velocystream client only
libraryDependencies += "com.bicou" %% "avokka-velocystream" % "0.0.5"

// or just the velocypack codec library
libraryDependencies += "com.bicou" %% "avokka-velocypack" % "0.0.5"
```
