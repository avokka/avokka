package avokka.velocypack.codecs

import avokka.velocypack.VPack.VNull
import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackNullCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  it should "encode to 0x18" in {
    assertCodec(vpackCodec, VNull, hex"18")
  }

  it should "fail if head is not null" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
