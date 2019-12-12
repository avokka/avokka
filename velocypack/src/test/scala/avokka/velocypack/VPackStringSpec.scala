package avokka.velocypack

import codecs.vpackCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackStringSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "empty string" should "encode to 0x40" in {
    assertCodec(vpackCodec, VPackString(""), hex"40")
  }

  "small strings" should "encode between 0x41 0xbe" in {
    assertCodec(vpackCodec, VPackString("@"), hex"4140")
    assertCodec(vpackCodec, VPackString("@@"), hex"424040")
  }

  "data length" should "be bytes length not string length" in {
    assertCodec(vpackCodec, VPackString("€"), hex"43e282ac")
    assertCodec(vpackCodec, VPackString("aфᐃ\uD835\uDD6B"), hex"4a61d184e19083f09d95ab")
  }

  "long strings" should "encode at 0xbf" in {
    val len = 300
    assertCodec(vpackCodec, VPackString("@" * len),
      hex"bf" ++ codecs.ulongBytes(len, 8).bytes ++ ByteVector.fill(len)(0x40)
    )
  }

  "codec" should "fail if head is not a string" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
