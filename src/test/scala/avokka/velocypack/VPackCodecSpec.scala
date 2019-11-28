package avokka.velocypack

import avokka.velocypack.VPackValue.{vpBool, vpDouble, vpInt, vpLong, codec => vpCodec}
import org.scalatest._
import scodec.bits._

class VPackCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "0x00" should "not be allowed in vpack values" in {
    assert(vpCodec.decode(hex"00".bits).isFailure)
  }

  "ints" should "encode to the most compact form" in {
    assertCodec(vpInt, 0, hex"30")
    assertCodec(vpInt, 1, hex"31")
    assertCodec(vpInt, -5, hex"3b")
    assertCodec(vpInt, 16, hex"2810")
  }

  "longs" should "encode from 0x20 to 0x3f" in {
    assertCodec(vpLong, -12L, hex"20 F4")
    assertCodec(vpLong, -30000L, hex"21 D08A")
    assertCodec(vpLong, 0xeeL, hex"28 ee")
    assertCodec(vpLong, 0xee11L, hex"29 11ee")
    assertCodec(vpLong, 0xee1122L, hex"2a 2211ee")
    assertCodec(vpLong, 0xee112233L, hex"2b 332211ee")
    assertCodec(vpLong, 0xee11223344L, hex"2c 44332211ee")
    assertCodec(vpLong, 0xee1122334455L, hex"2d 5544332211ee")
    assertCodec(vpLong, 0xee112233445566L, hex"2e 665544332211ee")
    assertCodec(vpLong, 0x0e11223344556677L, hex"2f 776655443322110e")
    assertCodec(vpLong, 0L, hex"30")
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
