---
layout: docs
title: Velocypack decoder
permalink: velocypack/decoder
---

# Velocypack decoder

[`VPackDecoder[T]`](/avokka/api/avokka/velocypack/VPackDecoder.html) decodes vpack values to scala values `VPack => Either[VPackError, T]`

By importing `avokka.velocypack._` you bring two helpers to `BitVector` having an implicit `VPackDecoder` :

* `asVPack[T]` for decoding a single type `T`

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

val bits = hex"02043334".bits

bits.asVPack[Vector[Long]]
```

* `asVPackSequence[T]` for decoding a stream of type `T`

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

val bitstream = hex"353637".bits

bitstream.asVPackSequence[Long]
```
