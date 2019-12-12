package avokka.velocypack

import avokka.velocypack.codecs.vpackCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackSmallintSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  it should "encode small integers 0, 1, ... 9 to 0x30-0x39" in {
    assertCodec(vpackCodec, VPackSmallint(0), hex"30")
    assertCodec(vpackCodec, VPackSmallint(1), hex"31")
    assertCodec(vpackCodec, VPackSmallint(2), hex"32")
    assertCodec(vpackCodec, VPackSmallint(3), hex"33")
    assertCodec(vpackCodec, VPackSmallint(4), hex"34")
    assertCodec(vpackCodec, VPackSmallint(5), hex"35")
    assertCodec(vpackCodec, VPackSmallint(6), hex"36")
    assertCodec(vpackCodec, VPackSmallint(7), hex"37")
    assertCodec(vpackCodec, VPackSmallint(8), hex"38")
    assertCodec(vpackCodec, VPackSmallint(9), hex"39")
  }

  it should "encode small negative integers -6, -5, ..., -1 to 0x3a-0x3f" in {
    assertCodec(vpackCodec, VPackSmallint(-6), hex"3a")
    assertCodec(vpackCodec, VPackSmallint(-5), hex"3b")
    assertCodec(vpackCodec, VPackSmallint(-4), hex"3c")
    assertCodec(vpackCodec, VPackSmallint(-3), hex"3d")
    assertCodec(vpackCodec, VPackSmallint(-2), hex"3e")
    assertCodec(vpackCodec, VPackSmallint(-1), hex"3f")
  }

  it should "fail if head is not a smallint" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
