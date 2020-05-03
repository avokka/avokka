package avokka.velocypack.codecs

import avokka.velocypack.VPack._
import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackBinaryCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  "data of length of 1 byte" should "encode to 0xc0" in {
    assertCodec(vpackCodec, VBinary(hex"aa"), hex"c0 01 aa")
  }

  "data of length of 2 bytes" should "encode to 0xc1" in {
    val c1 = ByteVector.fill(0x0100L)(0xaa)
    assertCodec(vpackCodec, VBinary(c1), hex"c1 0001" ++ c1)
  }

  "data of length of 3 bytes" should "encode to 0xc2" in {
    val c2 = ByteVector.fill(0x010203L)(0xbb)
    assertCodec(vpackCodec, VBinary(c2), hex"c2 030201" ++ c2)
  }

  "0xc3" should "decode as [4 bytes LE length][data]" in {
    assertDecode(vpackCodec, hex"c3 01 00 00 00 ff", VBinary(hex"ff"))
  }

  "0xc4" should "decode as [5 bytes LE length][data]" in {
    assertDecode(vpackCodec, hex"c4 01 00 00 00 00 ff", VBinary(hex"ff"))
  }

  "0xc5" should "decode as [6 bytes LE length][data]" in {
    assertDecode(vpackCodec, hex"c5 01 00 00 00 00 00 ff", VBinary(hex"ff"))
  }

  "0xc6" should "decode as [7 bytes LE length][data]" in {
    assertDecode(vpackCodec, hex"c6 01 00 00 00 00 00 00 ff", VBinary(hex"ff"))
  }

  "0xc7" should "decode as [8 bytes LE length][data]" in {
    assertDecode(vpackCodec, hex"c7 01 00 00 00 00 00 00 00 ff", VBinary(hex"ff"))
  }

  "roundtrip" should "not fail" in {
    forAll(genVBinary) { v: VBinary =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

  "codec" should "fail if head is not a binary" in {
    assert(VPackBinaryCodec.codec.decode(hex"00".bits).isFailure)
  }
}
