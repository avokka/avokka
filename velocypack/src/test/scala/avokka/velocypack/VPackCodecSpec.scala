package avokka.velocypack

import org.scalatest._
import scodec.bits._

class VPackCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "0x00" should "not be allowed in vpack values" in {
    assert(VPackValue.vpackCodec.decode(hex"00".bits).isFailure)
  }

}
