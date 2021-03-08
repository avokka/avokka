---
layout: docs
title: Velocypack encoder
permalink: velocypack/encoder
---

# Velocypack encoder

`VPackEncoder[T]` encodes scala values to VPack values `T => VPack`

By importing `avokka.velocypack._` you bring `toVPack` and `toVPackBits` to all types `T` having an implicit `VPackEncoder[T]` :

```scala mdoc:to-string
import avokka.velocypack._

val b: Boolean = true

b.toVPack
b.toVPackBits

val a: Seq[Int] = List(1,2)

a.toVPack
a.toVPackBits
```
