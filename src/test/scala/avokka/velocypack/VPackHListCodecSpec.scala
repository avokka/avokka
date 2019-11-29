package avokka.velocypack

import avokka.velocypack.codecs.VPackHListCodec
import cats.implicits._
import org.scalatest._
import scodec._
import scodec.bits._
import shapeless.{::, HNil}

class VPackHListCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  type R = String :: Boolean :: HNil
  implicit val request: Codec[R] = VPackHListCodec.codec[R]
  val requests = VPackHListCodec.codec[R :: R :: HNil]
  val compact = VPackHListCodec.codecCompact[Int :: Boolean :: HNil]
  val i3 = VPackHListCodec.codec[Int :: Int :: Int :: HNil]

  "empty array" should "encode to 0x01" in {
    val c = VPackHListCodec.codec[HNil]
    val result = c.encode(HNil)
    assert(result.isSuccessful)
    assertResult(hex"01")(result.require.bytes)
  }

  "array encoders" should "conform specs" in {

    // compact
    assertCodec(compact, 10 :: false :: HNil, hex"13 06 28 0a 19 02")
    assertEncodePack(compact, 10 :: false :: HNil, """[10,false]""")

    // indexed
    assertCodec(request, "a" :: true :: HNil, hex"06 08 02 41 61 1a 03 05")
    assertEncodePack(request, "a" :: true :: HNil, """["a",true]""")

    // same size elements
    assertCodec(request, "" :: true :: HNil, hex"02 04 40 1a")
    assertEncodePack(request, "" :: true :: HNil, """["",true]""")

    // recurse
    println(requests.encode(("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil))
    assertEncodePack(requests, ("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil,
      """[["a",true],["b",false]]"""
    )
  }

  "array decoders" should "conform specs" in {

    assert(i3.decode(hex"01".bits).isFailure)

    val ex = 1 :: 2 :: 3 :: HNil
    assertDecode(i3, hex"02 05 31 32 33", ex)
    assertDecode(i3, hex"03 06 00 31 32 33", ex)
    assertDecode(i3, hex"04 08 00 00 00 31 32 33", ex)
    assertDecode(i3, hex"05 0c 00 00 00 00 00 00 00 31 32 33", ex)
    assertDecode(i3, hex"06 09 03 31 32 33 03 04 05", ex)
    assertDecode(i3, hex"06 09 03 32 31 33 04 03 05", ex)
    assertDecode(i3, hex"07 0e 00 03 00 31 32 33 05 00 06 00 07 00", ex)
    assertDecode(i3, hex"08 18 00 00 00 03 00 00 00 31 32 33 09 00 00 00 0a 00 00 00 0b 00 00 00", ex)
    assertDecode(i3, hex"09 2c 00 00 00 00 00 00 00 31 32 33 09 00 00 00 00 00 00 00 0a 00 00 00 00 00 00 00 0b 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00", ex)

  }

}
