package avokka.velocypack

import avokka.velocypack.codecs.VPackBooleanCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackBooleanCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  it should "encode false to 0x19" in {
    assertCodec(VPackBooleanCodec, VPackBoolean(false), hex"19")
  }

  it should "encode true to 0x1a" in {
    assertCodec(VPackBooleanCodec, VPackBoolean(true), hex"1a")
  }

  it should "fail if head is not a boolean" in {
    assert(VPackBooleanCodec.decode(hex"00".bits).isFailure)
  }
}
