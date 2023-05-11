---
layout: docs
title: Velocypack case class
permalink: velocypack/case-class
---

## Case class generator

With the help of magnolia, we can instantiate codecs for case classes as velocypack objects (`VPackDecoder.gen`), or arrays (`VPackGeneric`) :

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

case class Test(b: Boolean)
implicit val testEncoder: VPackEncoder[Test] = VPackEncoder.gen
implicit val testDecoder: VPackDecoder[Test] = VPackDecoder.gen
```

Encoding :

```scala mdoc:to-string
val t = Test(true)

t.toVPack      
t.toVPackBits
```

Decoding :

```scala mdoc:to-string
hex"0b070141621903".bits.asVPack[Test]
hex"0a".bits.asVPack[Test]                                                                                         
```

Using defaults :

```scala mdoc:to-string
case class TestTrue(b: Boolean = true)        

implicit val testTrueDecoder: VPackDecoder[TestTrue] = VPackDecoder.gen

hex"0b070141621903".bits.asVPack[TestTrue]            
hex"0a".bits.asVPack[TestTrue]                                                                                                         
```

Automatic encoder and decoder derivation :

```scala mdoc:to-string


case class Element(name: String, value: Int)

case class Group(elements: Vector[Element])

Group(Vector(Element("a", 1), Element("b", 2))).toVPack
```