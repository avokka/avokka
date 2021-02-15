---
layout: docs
title: Installation
position: 2
permalink: installation
---

# Installation

SBT configuration :

```scala
// add dependency to the arangodb client
libraryDependencies += "com.bicou" %% "avokka-arangodb" % "@VERSION@"

// or the velocystream client only
libraryDependencies += "com.bicou" %% "avokka-velocystream" % "@VERSION@"

// or just the velocypack codec library
libraryDependencies += "com.bicou" %% "avokka-velocypack" % "@VERSION@"
```
