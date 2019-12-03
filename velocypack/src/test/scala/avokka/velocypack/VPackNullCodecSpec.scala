package avokka.velocypack

import avokka.velocypack.codecs.VPackNullCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackNullCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  it should "encode to 0x18" in {
    assertCodec(VPackNullCodec, VPackNull, hex"18")
  }

  it should "fail if head is not null" in {
    assert(VPackNullCodec.decode(hex"00".bits).isFailure)
  }
}
