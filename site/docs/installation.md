---
layout: docs
title: Installation
position: 2
permalink: installation
---

# Installation

Packages are published to bintray under the [`avokka`](https://bintray.com/avokka) organization.

SBT configuration :

```scala
// add avokka bintray repository to resolvers
resolvers += Resolver.bintrayRepo("avokka", "maven")

// add dependency to the arangodb client
libraryDependencies += "avokka" %% "avokka-arangodb" % "@VERSION@"

// or the velocystream client only
libraryDependencies += "avokka" %% "avokka-velocystream" % "@VERSION@"

// or just the velocypack codec library
libraryDependencies += "avokka" %% "avokka-velocypack" % "@VERSION@"
```
