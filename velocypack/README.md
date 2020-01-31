# avokka-velocypack

Velocypack encoder and decoder in pure scala

- `VPackEncoder` encodes scala values to VPack values `T => VPack`
- `VPackDecoder` decodes vpack values to scala values `VPack => Either[VPackError, T]`

The *codecs* package includes the scodec codecs for VPack values: `VPack => Attempt[BitVector]` and `BitVector => Attempt[DecodeResult[VPack]]`

# Usage

```scala
import avokka.velocypack._   // import avokka.velocypack._

val b: Boolean = true        // b: Boolean = true
b.toVPack                    // res0: avokka.velocypack.VPack = VBoolean(true)
b.toVPackBits                // res1: scala.util.Either[avokka.velocypack.VPackError,scodec.bits.BitVector] = Right(BitVector(8 bits, 0x1a))

val a: Seq[Int] = List(1,2)  // a: Seq[Int] = List(1, 2)
a.toVPack                    // res2: avokka.velocypack.VPack = VArray(Chain(VSmallint(1), VSmallint(2)))
a.toVPackBits                // res3: scala.util.Either[avokka.velocypack.VPackError,scodec.bits.BitVector] = Right(BitVector(32 bits, 0x02043132))

import scodec.bits._         // import scodec.bits._

val bits = BitVector(hex"02043334") // bits: scodec.bits.BitVector = BitVector(32 bits, 0x02043334)
bits.asVPack[Vector[Long]]          // res4: scala.util.Either[avokka.velocypack.VPackError,scodec.DecodeResult[Vector[Long]]] = Right(DecodeResult(Vector(3, 4),BitVector(empty)))

```
