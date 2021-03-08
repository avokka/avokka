---
layout: docs
title: ArangoDB AQL
permalink: arangodb/aql
---

# AQL string interpolation

Package `avokka.arangodb.aql` contains a custom string interpolator to build [AQL](https://www.arangodb.com/docs/stable/aql/index.html) queries.

```scala mdoc
import avokka.arangodb.aql._

aql"FOR u IN users RETURN u"
```

Use `$` to bind variables to query string :

```scala mdoc
val active: Boolean = true

aql"FOR u IN users FILTER u.active == $active RETURN u"
```

Mixing with `.bind` :

```scala mdoc
aql"FOR u IN users FILTER u.active == $active && u.level == @level RETURN u".bind("level", "newbie")
```