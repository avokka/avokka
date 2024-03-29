package avokka.velocypack
package codecs

import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackLongCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  "negative longs" should "encode to 0x20-0x27 as signed int, little endian, 1 to 8 bytes (V - 0x1f), two's complement" in {

    assertCodec(vpackCodec, VLong(-1L),         hex"20 ff")
    assertCodec(vpackCodec, VLong(-(1L << 8)),  hex"21 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 16)), hex"22 00 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 24)), hex"23 00 00 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 32)), hex"24 00 00 00 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 40)), hex"25 00 00 00 00 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 48)), hex"26 00 00 00 00 00 00 ff")
    assertCodec(vpackCodec, VLong(-(1L << 56)), hex"27 00 00 00 00 00 00 00 ff")

    assertCodec(vpackCodec, VLong(-30000L), hex"21 D08A")
  }

  "positive longs" should "encode to 0x28-0x2f as unsigned int, little endian, 1 to 8 bytes (V - 0x27)" in {
    assertCodec(vpackCodec, VLong(0xeeL), hex"28 ee")
    assertCodec(vpackCodec, VLong(0xee11L), hex"29 11ee")
    assertCodec(vpackCodec, VLong(0xee1122L), hex"2a 2211ee")
    assertCodec(vpackCodec, VLong(0xee112233L), hex"2b 332211ee")
    assertCodec(vpackCodec, VLong(0xee11223344L), hex"2c 44332211ee")
    assertCodec(vpackCodec, VLong(0xee1122334455L), hex"2d 5544332211ee")
    assertCodec(vpackCodec, VLong(0xee112233445566L), hex"2e 665544332211ee")
    assertCodec(vpackCodec, VLong(0x0e11223344556677L), hex"2f 776655443322110e")
  }

  "roundtrip" should "not fail" in {
    forAll(genVLong) { (v: VLong) =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

  "codec" should "fail if head is not a long" in {
    assert(VPackLongCodec.codec.decode(hex"00".bits).isFailure)
  }
}
