package avokka.velocypack
package codecs

import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  "0x00" should "not be allowed in vpack values" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }

  "roundtrip" should "not fail" in {
    forAll(genV) { v: VPack =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

}
