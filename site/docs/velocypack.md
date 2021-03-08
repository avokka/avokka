---
layout: docs
title: Velocypack
position: 3
permalink: velocypack
---

# Velocypack

Velocypack encoder and decoder in pure scala à la circe

The *codecs* package includes the scodec codecs for VPack values: `VPack => Attempt[BitVector]` and `BitVector => Attempt[DecodeResult[VPack]]`

## Installation

SBT configuration :

```scala
libraryDependencies += "com.bicou" %% "avokka-velocypack" % "@VERSION@"
```

## Supported types

| Scala / Java                                                                 | avokka VPack              | Velocypack                                  |
|:-----------------------------------------------------------------------------|:--------------------------|:--------------------------------------------|
| Boolean                                                                      | VBoolean                  | true or false                               |
| Long Short, Int                                                              | VLong, VSmallInt          | signed int, unsigned int, small int         |
| Double                                                                       | VDouble, VLong, VSmallInt | double, signed int, unsigned int, small int |
| String                                                                       | VString                   | small string, long string                   |
| Instant                                                                      | VDate, VLong, VString     | utc date, int(millis), string(iso)          |
| Array\[Byte\], ByteVector                                                    | VBinary, VString          | binary blob, string(hex)                    |
| UUID                                                                         | VBinary, VString          | binary blob, string                         |
| Option\[T\]                                                                  | VNull, VPack              | null, vpack                                 |
| Vector\[T\], List\[T\], Seq\[T\], Set\[T\], Iterable\[T\], Chain\[T\], HList | VArray                    | array                                       |
| Map\[String, T\], case classes                                               | VObject                   | object                                      |
| Unit                                                                         | VNone                     | _empty_                                     |

