package avokka.velocypack

import avokka.velocypack.VPackValue.{vpBool, vpString, vpInt}
import avokka.velocypack.VelocypackArrayCodec.{codec, codecCompact}
import com.arangodb.velocypack.VPackSlice
import org.scalatest._
import scodec._
import scodec.bits._
import shapeless.{::, HNil}

class VelocypackArrayCodecSpec extends FlatSpec with Matchers {

  val request: Codec[String :: Boolean :: HNil] = codec(vpString :: vpBool :: HNil)
  val requests = codec(request :: request :: HNil)
  val compact = codecCompact(vpInt :: vpBool :: HNil)

  "empty array" should "encode to 0x01" in {
    val c = codec[HNil, HNil](HNil)
    val result = c.encode(HNil)
    assert(result.isSuccessful)
    assertResult(hex"01")(result.require.bytes)
  }

  "array codec" should "accept only codecs" in {
    assertTypeError("codec(String :: HNil)")
    assertCompiles("codec(VPackValue.vpBool :: HNil)")
  }

  "simple array codec" should "return an codec" in {
    val c = codec(VPackValue.vpBool :: HNil)
    assert(c.isInstanceOf[Codec[Boolean :: HNil]])
  }

  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)


  def assertEncode[T](c: Codec[T], v: T, b: ByteVector) = {
    assertResult(b)(c.encode(v).require.bytes)
  }
  def assertEncodePack[T](c: Codec[T], v: T, json: String) = {
    val r = c.encode(v).require
    assertResult(json)(toSlice(r).toString)
  }

  def assertDecode[T](c: Codec[T], b: ByteVector, v: T) = {
    assertResult(v)(c.decode(b.bits).require.value)
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

    val ib = codec(VPackValue.vpInt :: VPackValue.vpBool :: HNil)
    assertDecode(ib, hex"02 05 00 31 19", 1 :: false :: HNil)

    val c = codec(VPackValue.vpInt :: VPackValue.vpInt :: VPackValue.vpInt :: HNil)
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

    val c2 = codec(VPackValue.vpInt :: VPackValue.vpInt :: HNil)
    assertDecode(c2, hex"13 06 31 28 10 02", 1 :: 16 :: HNil)
  }

}
