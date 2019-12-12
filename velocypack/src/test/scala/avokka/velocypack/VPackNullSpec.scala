package avokka.velocypack

import VPack._
import codecs.vpackCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackNullSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  it should "encode to 0x18" in {
    assertCodec(vpackCodec, VNull, hex"18")
  }

  it should "fail if head is not null" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
