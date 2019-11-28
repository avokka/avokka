package avokka.velocypack

import avokka.velocypack.codecs.VPackSmallintCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackSmallintCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "small integers 0, 1, ... 9" should "encode to 0x30-0x39" in {
    assertCodec(VPackSmallintCodec, VPackSmallint(0), hex"30")
    assertCodec(VPackSmallintCodec, VPackSmallint(1), hex"31")
    assertCodec(VPackSmallintCodec, VPackSmallint(2), hex"32")
    assertCodec(VPackSmallintCodec, VPackSmallint(3), hex"33")
    assertCodec(VPackSmallintCodec, VPackSmallint(4), hex"34")
    assertCodec(VPackSmallintCodec, VPackSmallint(5), hex"35")
    assertCodec(VPackSmallintCodec, VPackSmallint(6), hex"36")
    assertCodec(VPackSmallintCodec, VPackSmallint(7), hex"37")
    assertCodec(VPackSmallintCodec, VPackSmallint(8), hex"38")
    assertCodec(VPackSmallintCodec, VPackSmallint(9), hex"39")
  }

  "small negative integers -6, -5, ..., -1" should "encode to 0x3a-0x3f" in {
    assertCodec(VPackSmallintCodec, VPackSmallint(-6), hex"3a")
    assertCodec(VPackSmallintCodec, VPackSmallint(-5), hex"3b")
    assertCodec(VPackSmallintCodec, VPackSmallint(-4), hex"3c")
    assertCodec(VPackSmallintCodec, VPackSmallint(-3), hex"3d")
    assertCodec(VPackSmallintCodec, VPackSmallint(-2), hex"3e")
    assertCodec(VPackSmallintCodec, VPackSmallint(-1), hex"3f")
  }

  "codec" should "fail if head is not a smallint" in {
    assert(VPackSmallintCodec.decode(hex"00".bits).isFailure)
  }
}
