package avokka.velocypack

import avokka.velocypack.codecs.{VPackArrayCodec, VPackHListCodec}
import avokka.velocypack.codecs.VPackHList2Codec.{codec, codecCompact}
import cats.implicits._
import org.scalatest._
import scodec._
import scodec.bits._
import shapeless.{::, HNil}

class VPackHListCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  val request: Codec[String :: Boolean :: HNil] = VPackHListCodec.codec[String :: Boolean :: HNil]
  val requests = codec(request :: request :: HNil)
  val compact = codecCompact(intCodec :: booleanCodec :: HNil)

  "empty array" should "encode to 0x01" in {
    val c = codec[HNil, HNil](HNil)
    val result = c.encode(HNil)
    assert(result.isSuccessful)
    assertResult(hex"01")(result.require.bytes)
  }

  "array codec" should "accept only codecs" in {
    assertTypeError("codec(String :: HNil)")
    assertCompiles("codec(booleanCodec :: HNil)")
  }

  "simple array codec" should "return an codec" in {
    val c = codec(booleanCodec :: HNil)
    assert(c.isInstanceOf[Codec[Boolean :: HNil]])
  }

  "array encoders" should "conform specs" in {
    assertEncode(compact, 10 :: false :: HNil, hex"13 06 28 0a 19 02")
    assertEncodePack(compact, 10 :: false :: HNil, """[10,false]""")

    assertEncode(request, "a" :: true :: HNil, hex"06 08 02 41 61 1a 03 05")
    assertEncodePack(request, "a" :: true :: HNil, """["a",true]""")

    // same size elements
    assertEncode(request, "" :: true :: HNil, hex"02 04 40 1a")
    assertEncodePack(request, "" :: true :: HNil, """["",true]""")

    assertEncodePack(requests, ("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil,
      """[["a",true],["b",false]]"""
    )
  }

  "array decoders" should "conform specs" in {

    val ib = codec(intCodec :: booleanCodec :: HNil)
    assertDecode(ib, hex"02 05 00 31 19", 1 :: false :: HNil)

    val c = codec(intCodec :: intCodec :: intCodec :: HNil)
    assert(c.decode(hex"01".bits).isFailure)

    val ex = 1 :: 2 :: 3 :: HNil
    assertDecode(c, hex"02 05 31 32 33", ex)
    assertDecode(c, hex"03 06 00 31 32 33", ex)
    assertDecode(c, hex"04 08 00 00 00 31 32 33", ex)
    assertDecode(c, hex"05 0c 00 00 00 00 00 00 00 31 32 33", ex)
    assertDecode(c, hex"06 09 03 31 32 33 03 04 05", ex)
    assertDecode(c, hex"06 09 03 32 31 33 04 03 05", ex)
    assertDecode(c, hex"07 0e 00 03 00 31 32 33 05 00 06 00 07 00", ex)
    assertDecode(c, hex"08 18 00 00 00 03 00 00 00 31 32 33 09 00 00 00 0a 00 00 00 0b 00 00 00", ex)
    assertDecode(c, hex"09 2c 00 00 00 00 00 00 00 31 32 33 09 00 00 00 00 00 00 00 0a 00 00 00 00 00 00 00 0b 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00", ex)

    val c2 = codec(intCodec :: intCodec :: HNil)
    assertDecode(c2, hex"13 06 31 28 10 02", 1 :: 16 :: HNil)
  }

  "vector codec" should "conform specs" in {
    val lint = vectorCodec(intCodec)
    assertCodec(lint, Vector(1,2,3), hex"02 05 31 32 33")

    val cint = VPackArrayCodec.Compact.traverse[Int, Vector](intCodec)
    assertCodec(cint, Vector(0,1,2), hex"13 06 30 31 32 03")
  }

  "list codec" should "conform specs" in {
    val lint = listCodec(intCodec)

    assertCodec(lint, List(1,2,3), hex"02 05 31 32 33")
    assertCodec(lint, List(16,32,64,128), hex"02 0a 2810 2820 2840 2880")
    assertCodec(lint, List(1,16), hex"06 08 02 31 2810 03 04")
    assertCodec(lint, List(16,1), hex"06 08 02 2810 31 03 05")
    assertDecode(lint, hex"06 09 02 00 2810 31 04 06", List(16,1))

  }

}
