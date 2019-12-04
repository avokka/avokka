package avokka.velocypack

import avokka.velocypack.codecs.VPackArrayCodec
import cats.implicits._
import org.scalatest._
import scodec.bits._

class VPackArrayCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  val a1false = VPackArray(Seq(hex"31".bits, hex"19".bits))
  val a10false = VPackArray(Seq(hex"28 0a".bits, hex"19".bits))
  val avoidtrue = VPackArray(Seq(hex"40".bits, hex"1a".bits))
  val a123 = VPackArray(Seq(hex"31".bits, hex"32".bits, hex"33".bits))
  val bigArray = VPackArray(Seq.fill(1000)(hex"30".bits))
  val bigArraJson: String = "[" + Seq.fill(1000)("0").mkString(",") + "]"

  "empty array" should "encode to 0x01" in {
    assertCodec(VPackArrayCodec, VPackArray(List.empty), hex"01")
  }

  "same size elements" should "encode at 0x02-0x05" in {
    assertCodec(VPackArrayCodec, avoidtrue, hex"02 04 40 1a")
    assertEncodePack(VPackArrayCodec, avoidtrue, """["",true]""")
    assertEncodePack(VPackArrayCodec, bigArray, bigArraJson)
    assertEncodeDecode(VPackArrayCodec, bigArray)
  }

  "array with index table" should "encode at 0x06-0x09" in {
    assertCodec(VPackArrayCodec, a10false, hex"06 08 02 28 0a 19 03 05")
    assertEncodePack(VPackArrayCodec, a10false, """[10,false]""")
  }

  "compact array" should "encode at 0x13" in {
    assertCodec(VPackArrayCodec.Compact, a10false, hex"13 06 28 0a 19 02")
    assertEncodePack(VPackArrayCodec.Compact, a10false, """[10,false]""")
    assertEncodePack(VPackArrayCodec.Compact, bigArray, bigArraJson)
    assertEncodeDecode(VPackArrayCodec.Compact, bigArray)
  }

  "optional unused padding" should "be properly ignored" in {
    assertDecode(VPackArrayCodec, hex"02 04 31 19", a1false)
    assertDecode(VPackArrayCodec, hex"02 05 00 31 19", a1false)
    assertDecode(VPackArrayCodec, hex"02 06 00 00 31 19", a1false)
    assertDecode(VPackArrayCodec, hex"02 07 00 00 00 31 19", a1false)
  }

  "byte length" should "be properly decoded" in {
    assertDecode(VPackArrayCodec, hex"02 05 31 32 33", a123)
    assertDecode(VPackArrayCodec, hex"03 06 00 31 32 33", a123)
    assertDecode(VPackArrayCodec, hex"04 08 00 00 00 31 32 33", a123)
    assertDecode(VPackArrayCodec, hex"05 0c 00 00 00 00 00 00 00 31 32 33", a123)
    assertDecode(VPackArrayCodec, hex"06 09 03 31 32 33 03 04 05", a123)
    assertDecode(VPackArrayCodec, hex"06 09 03 32 31 33 04 03 05", a123)
    assertDecode(VPackArrayCodec, hex"07 0e 00 03 00 31 32 33 05 00 06 00 07 00", a123)
    assertDecode(VPackArrayCodec, hex"08 18 00 00 00 03 00 00 00 31 32 33 09 00 00 00 0a 00 00 00 0b 00 00 00", a123)
    assertDecode(VPackArrayCodec, hex"09 2c 00 00 00 00 00 00 00 31 32 33 09 00 00 00 00 00 00 00 0a 00 00 00 00 00 00 00 0b 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00", a123)
  }

  "codec from vector" should "encode vectors" in {
    val lint = vectorCodec(intCodec)
    assertCodec(lint, Vector(1,2,3), hex"02 05 31 32 33")

    val cint = VPackArrayCodec.Compact.traverse[Int, Vector](intCodec)
    assertCodec(cint, Vector(0,1,2), hex"13 06 30 31 32 03")
  }

  "codec from list" should "encode lists" in {
    val lint = listCodec(intCodec)
    assertCodec(lint, List(1,2,3), hex"02 05 31 32 33")
    assertCodec(lint, List(16,32,64,128), hex"02 0a 2810 2820 2840 2880")
    assertCodec(lint, List(1,16), hex"06 08 02 31 2810 03 04")
    assertCodec(lint, List(16,1), hex"06 08 02 2810 31 03 05")
    assertDecode(lint, hex"06 09 02 00 2810 31 04 06", List(16,1))
  }

  "codec" should "fail if head is not a array" in {
    assert(VPackArrayCodec.decode(hex"00".bits).isFailure)
  }
}
