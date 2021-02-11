# avokka-velocypack

Velocypack encoder and decoder in pure scala

- `VPackEncoder` encodes scala values to VPack values `T => VPack`
- `VPackDecoder` decodes vpack values to scala values `VPack => Either[VPackError, T]`

The *codecs* package includes the scodec codecs for VPack values: `VPack => Attempt[BitVector]` and `BitVector => Attempt[DecodeResult[VPack]]`

## Installation

SBT configuration :

@@@ vars

```sbt
// avokka is published at bintray
resolvers += Resolver.bintrayRepo("avokka", "maven")

libraryDependencies += "avokka" %% "avokka-velocypack" % "$version$"
```

@@@

## Usage

By importing `avokka.velocypack._` you bring `asVPack` and `asVPackBits` to all types `T` having an implicit `VPackEncoder[T]` :

```scala mdoc:to-string
import avokka.velocypack._

val b: Boolean = true

b.toVPack
b.toVPackBits

val a: Seq[Int] = List(1,2)

a.toVPack
a.toVPackBits
```

Decoding from BitVector is the opposite transformation whith an implicit `VPackDecoder[T]` :

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

val bits = hex"02043334".bits
bits.asVPack[Vector[Long]]
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

## Case class derivation

With the help of magnolia, we can instantiate codecs for case classes as velocypack objects (`VPackDecoder.gen`), or arrays (`VPackGeneric`) :

```scala mdoc:to-string
import avokka.velocypack._
import scodec.bits._

case class Test(b: Boolean)
implicit val testEncoder: VPackEncoder[Test] = VPackEncoder.gen
implicit val testDecoder: VPackDecoder[Test] = VPackDecoder.gen

val t = Test(true)

t.toVPack      
t.toVPackBits

hex"0b070141621903".bits.asVPack[Test]
hex"0a".bits.asVPack[Test]                                                                                         

case class TestTrue(b: Boolean = true)        

implicit val testTrueDecoder: VPackDecoder[TestTrue] = VPackDecoder.gen

hex"0b070141621903".bits.asVPack[TestTrue]            
hex"0a".bits.asVPack[TestTrue]                                                                                                         
```
