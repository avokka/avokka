package avokka.velocypack
package codecs

import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackBooleanCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  it should "encode false to 0x19" in {
    assertCodec(vpackCodec, VFalse, hex"19")
  }

  it should "encode true to 0x1a" in {
    assertCodec(vpackCodec, VTrue, hex"1a")
  }

  it should "fail if head is not a boolean" in {
    assert(VPackBooleanCodec.codec.decode(hex"00".bits).isFailure)
  }
}
