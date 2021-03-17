---
layout: docs
title: Velocypack decoder
permalink: velocypack/decoder
---

# Velocypack decoder

[`VPackDecoder[T]`](/avokka/api/avokka/velocypack/VPackDecoder.html) decodes vpack values to scala values `VPack => Either[VPackError, T]`

By importing `avokka.velocypack._` you bring `asVPack[T]` to `BitVector` for all types `T` having an implicit `VPackDecoder[T]` :

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

val bits = hex"02043334".bits

bits.asVPack[Vector[Long]]
```
