---
layout: docs
title: Installation
position: 2
permalink: installation
---

# Installation

Packages are published to bintray under the [`avokka`](https://bintray.com/avokka) organization.

SBT configuration :

@@@ vars
```scala
// add avokka bintray repository to resolvers
resolvers += Resolver.bintrayRepo("avokka", "maven")

// add dependency to the arangodb client
libraryDependencies += "avokka" %% "avokka-arangodb" % "$version$"

// or the velocystream client only
libraryDependencies += "avokka" %% "avokka-velocystream" % "$version$"

// or just the velocypack codec library
libraryDependencies += "avokka" %% "avokka-velocypack" % "$version$"
```
@@@