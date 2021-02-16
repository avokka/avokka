# avokka-velocypack

Velocypack encoder and decoder in pure scala

- `VPackEncoder` encodes scala values to VPack values `T => VPack`
- `VPackDecoder` decodes vpack values to scala values `VPack => Either[VPackError, T]`

The *codecs* package includes the scodec codecs for VPack values: `VPack => Attempt[BitVector]` and `BitVector => Attempt[DecodeResult[VPack]]`

## Usage

By importing `avokka.velocypack._` you bring `asVPack` and `asVPackBits` to all types `T` having an implicit `VPackEncoder[T]` :

```scala
import avokka.velocypack._       // import avokka.velocypack._

val b: Boolean = true            // b: Boolean = true

b.toVPack                        // res0: avokka.velocypack.VPack = VBoolean(true)
b.toVPackBits.right.get          // res1: scodec.bits.BitVector = BitVector(8 bits, 0x1a)

val a: Seq[Int] = List(1,2)      // a: Seq[Int] = List(1, 2)

a.toVPack                        // res2: avokka.velocypack.VPack = VArray(Chain(VSmallint(1), VSmallint(2)))
a.toVPackBits.right.get          // res3: scodec.bits.BitVector = BitVector(32 bits, 0x02043132)
```

Decoding from BitVector is the opposite transformation whith an implicit `VPackDecoder[T]` :

```scala
import avokka.velocypack._       // import avokka.velocypack._
import scodec.bits._             // import scodec.bits._

val bits = hex"02043334".bits                // bits: scodec.bits.BitVector = BitVector(32 bits, 0x02043334)
bits.asVPack[Vector[Long]].right.get.value   // res4: Vector[Long] = Vector(3, 4)
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

With the help of shapeless, we can instantiate codecs for case classes as velocypack objects (`VPackRecord`), or arrays (`VPackGeneric`) :

```scala
import avokka.velocypack._                                                       // import avokka.velocypack._
import scodec.bits._                                                             // import scodec.bits._

case class Test(b: Boolean)                                                      // defined class Test
implicit val testEncoder: VPackEncoder[Test] = VPackRecord[Test].encoder         // testEncoder: avokka.velocypack.VPackEncoder[Test] = ...
implicit val testDecoder: VPackDecoder[Test] = VPackRecord[Test].decoder         // testDecoder: avokka.velocypack.VPackDecoder[Test] = ...

val t = Test(true)                                                               // t: Test = Test(true)

t.toVPack                                                                        // res5: avokka.velocypack.VPack = VObject(Map(b -> VBoolean(true)))
t.toVPackBits.right.get                                                          // res6: scodec.bits.BitVector = BitVector(56 bits, 0x0b070141621a03)

hex"0b070141621903".bits.asVPack[Test].right.get.value                           // res7: Test = Test(false)
hex"0a".bits.asVPack[Test].left.get                                              // res8: avokka.velocypack.VPackError = ObjectFieldAbsent(b,List())                                                                              

case class TestTrue(b: Boolean = true)                                           // defined class TestTrue

implicit val testTrueDecoder: VPackDecoder[TestTrue] = VPackRecord[TestTrue].decoderWithDefaults //testTrueDecoder: avokka.velocypack.VPackDecoder[TestTrue] = ...

hex"0b070141621903".bits.asVPack[TestTrue].right.get.value                       // res9: TestTrue = TestTrue(false)
hex"0a".bits.asVPack[TestTrue].right.get.value                                   // res10: TestTrue = TestTrue(true)                                                                                
```
