package avokka.velocypack

import codecs.vpackCodec
import org.scalatest._
import scodec.bits._

class VPackCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "0x00" should "not be allowed in vpack values" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }

}
