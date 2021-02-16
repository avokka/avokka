package avokka.velocypack
package codecs

import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackStringCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  "empty string" should "encode to 0x40" in {
    assertCodec(vpackCodec, VString(""), hex"40")
  }

  "small strings" should "encode between 0x41 0xbe" in {
    assertCodec(vpackCodec, VString("@"), hex"4140")
    assertCodec(vpackCodec, VString("@@"), hex"424040")
  }

  "data length" should "be bytes length not string length" in {
    assertCodec(vpackCodec, VString("€"), hex"43e282ac")
    assertCodec(vpackCodec, VString("aфᐃ\uD835\uDD6B"), hex"4a61d184e19083f09d95ab")
  }

  "long strings" should "encode at 0xbf" in {
    val len = 300
    assertCodec(vpackCodec, VString("@" * len),
      hex"bf" ++ codecs.ulongBytes(len.toLong, 8).bytes ++ ByteVector.fill(len.toLong)(0x40)
    )
  }

  "roundtrip" should "not fail" in {
    forAll(genVString) { v: VString =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

  "codec" should "fail if head is not a string" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
