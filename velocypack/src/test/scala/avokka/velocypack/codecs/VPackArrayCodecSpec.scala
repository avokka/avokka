package avokka.velocypack.codecs

import avokka.velocypack.VPack._
import cats.data.Chain
import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackArrayCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  val a1false = VArray(VSmallint(1), VFalse)
  val a10false = VArray(VLong(10),  VFalse)
  val avoidtrue = VArray(VString(""),  VTrue)
  val a123 = VArray(VSmallint(1), VSmallint(2), VSmallint(3))
  val bigArray = VArray(Chain.fromSeq(Vector.fill(1000)(VSmallint(0))))
  val bigArraJson: String = "[" + Seq.fill(1000)("0").mkString(",") + "]"

  "empty array" should "encode to 0x01" in {
    assertEncode(vpackCodec, VArray(), hex"01")
  }

  "same size elements" should "encode at 0x02-0x05" in {
    assertCodec(vpackCodec, avoidtrue, hex"02 04 40 1a")
    assertEncodePack(vpackCodec, avoidtrue, """["",true]""")
    assertEncodePack(vpackCodec, bigArray, bigArraJson)
    assertEncodeDecode(vpackCodec, bigArray)
  }

  "array with index table" should "encode at 0x06-0x09" in {
    assertCodec(VPackArrayCodec.codec, a10false, hex"06 08 02 28 0a 19 03 05")
    assertEncodePack(VPackArrayCodec.encoder, a10false, """[10,false]""")
  }

  "compact array" should "encode at 0x13" in {
    assertCodec(VPackArrayCodec.codecCompact, a10false, hex"13 06 28 0a 19 02")
    assertEncodePack(VPackArrayCodec.encoderCompact, a10false, """[10,false]""")
    assertEncodePack(VPackArrayCodec.encoderCompact, bigArray, bigArraJson)
    assertEncodeDecode(VPackArrayCodec.codecCompact, bigArray)
  }

  "optional unused padding" should "be properly ignored" in {
    assertDecode(vpackCodec, hex"02 04 31 19", a1false)
    assertDecode(vpackCodec, hex"02 05 00 31 19", a1false)
    assertDecode(vpackCodec, hex"02 06 00 00 31 19", a1false)
    assertDecode(vpackCodec, hex"02 07 00 00 00 31 19", a1false)
  }

  "byte length" should "be properly decoded" in {
    assertDecode(vpackCodec, hex"02 05 31 32 33", a123)
    assertDecode(vpackCodec, hex"03 06 00 31 32 33", a123)
    assertDecode(vpackCodec, hex"04 08 00 00 00 31 32 33", a123)
    assertDecode(vpackCodec, hex"05 0c 00 00 00 00 00 00 00 31 32 33", a123)
    assertDecode(vpackCodec, hex"06 09 03 31 32 33 03 04 05", a123)
    assertDecode(vpackCodec, hex"06 09 03 32 31 33 04 03 05", a123)
    assertDecode(vpackCodec, hex"07 0e 00 03 00 31 32 33 05 00 06 00 07 00", a123)
    assertDecode(vpackCodec, hex"08 18 00 00 00 03 00 00 00 31 32 33 09 00 00 00 0a 00 00 00 0b 00 00 00", a123)
    assertDecode(vpackCodec, hex"09 2c 00 00 00 00 00 00 00 31 32 33 09 00 00 00 00 00 00 00 0a 00 00 00 00 00 00 00 0b 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00", a123)
  }

  "roundtrip" should "not fail" in {
    forAll(genVArray()) { v: VArray =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

  "codec" should "fail if head is not a array" in {
    assert(VPackArrayCodec.codec.decode(hex"00".bits).isFailure)
  }
}
