package avokka.velocypack

import avokka.velocypack.codecs.VPackBinaryCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackBinaryCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "data of length of 1 byte" should "encode to 0xc0" in {
    assertCodec(VPackBinaryCodec, VPackBinary(hex"aa"), hex"c0 01 aa")
  }

  "data of length of 2 bytes" should "encode to 0xc1" in {
    val c1 = ByteVector.fill(0x0100L)(0xaa)
    assertCodec(VPackBinaryCodec, VPackBinary(c1), hex"c1 0001" ++ c1)
  }

  "data of length of 3 bytes" should "encode to 0xc2" in {
    val c2 = ByteVector.fill(0x010203L)(0xbb)
    assertCodec(VPackBinaryCodec, VPackBinary(c2), hex"c2 030201" ++ c2)
  }

  "0xc3" should "decode as [4 bytes LE length][data]" in {
    assertDecode(VPackBinaryCodec, hex"c3 01 00 00 00 ff", VPackBinary(hex"ff"))
  }

  "0xc4" should "decode as [5 bytes LE length][data]" in {
    assertDecode(VPackBinaryCodec, hex"c4 01 00 00 00 00 ff", VPackBinary(hex"ff"))
  }

  "0xc5" should "decode as [6 bytes LE length][data]" in {
    assertDecode(VPackBinaryCodec, hex"c5 01 00 00 00 00 00 ff", VPackBinary(hex"ff"))
  }

  "0xc6" should "decode as [7 bytes LE length][data]" in {
    assertDecode(VPackBinaryCodec, hex"c6 01 00 00 00 00 00 00 ff", VPackBinary(hex"ff"))
  }

  "0xc7" should "decode as [8 bytes LE length][data]" in {
    assertDecode(VPackBinaryCodec, hex"c7 01 00 00 00 00 00 00 00 ff", VPackBinary(hex"ff"))
  }

  "codec" should "fail if head is not a binary" in {
    assert(VPackBinaryCodec.decode(hex"00".bits).isFailure)
  }
}
