package avokka.velocypack

import avokka.velocypack.VPackValue.{vpBool, vpDouble, vpInt, vpLong, codec => vpCodec}
import org.scalatest._
import scodec.bits._

class VPackCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "0x00" should "not be allowed in vpack values" in {
    assert(vpCodec.decode(hex"00".bits).isFailure)
  }

  "double" should "encode at 0x1b" in {
    assertCodec(vpDouble, 1.2d,
      hex"1b" ++ ByteVector.fromLong(java.lang.Double.doubleToRawLongBits(1.2), 8, ByteOrdering.LittleEndian)
    )
    assertCodec(vpDouble, 1.5d, hex"1b 000000000000F83F")
    assertCodec(vpDouble, -1.5d, hex"1b 000000000000F8BF")
    assertCodec(vpDouble, 1.23456789d, hex"1b 1B DE 83 42 CA C0 F3 3F")
    assertCodec(vpDouble, 0d, hex"30")
    assertCodec(vpDouble, 0.001d, hex"1bfca9f1d24d62503f")
    assertCodec(vpDouble, 10d, hex"280a")
  }
}
