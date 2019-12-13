package avokka.velocypack.codecs

import avokka.velocypack.VPack._
import avokka.velocypack.codecs
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scodec.bits._

class VPackDoubleCodecSpec extends FlatSpec with Matchers with ScalaCheckDrivenPropertyChecks with VPackCodecSpecTrait {

  it should "encode at 0x1b" in {
    forAll { d: Double =>
      val lbits = java.lang.Double.doubleToRawLongBits(d)
      assertCodec(vpackCodec, VDouble(d),
        hex"1b" ++ codecs.ulongBytes(lbits, 8).bytes
      )
    }

    assertCodec(vpackCodec, VDouble(1.5d), hex"1b 000000000000F83F")
    assertCodec(vpackCodec, VDouble(-1.5d), hex"1b 000000000000F8BF")
    assertCodec(vpackCodec, VDouble(1.23456789d), hex"1b 1B DE 83 42 CA C0 F3 3F")
    assertCodec(vpackCodec, VDouble(0.001d), hex"1bfca9f1d24d62503f")

  }

  // TODO: move this to Codec[Double] assertCodec(vpDouble, 0d, hex"30")
  // TODO: move this to Codec[Double] assertCodec(vpDouble, 10d, hex"280a")

  it should "fail if head is not a double" in {
    assert(VPackDoubleCodec.codec.decode(hex"00".bits).isFailure)
  }
}
