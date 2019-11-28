package avokka.velocypack

import avokka.velocypack.codecs.VPackBooleanCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackBooleanCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "false" should "be 0x19" in {
    assertCodec(VPackBooleanCodec, VPackBoolean(false), hex"19")
  }

  "true" should "be 0x1a" in {
    assertCodec(VPackBooleanCodec, VPackBoolean(true), hex"1a")
  }

  "codec" should "fail if head is not a boolean" in {
    assert(VPackBooleanCodec.decode(hex"00".bits).isFailure)
  }
}
