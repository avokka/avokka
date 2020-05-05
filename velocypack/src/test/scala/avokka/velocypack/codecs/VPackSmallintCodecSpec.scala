package avokka.velocypack.codecs

import avokka.velocypack.VPack.VSmallint
import org.scalatest.flatspec.AnyFlatSpec
import scodec.bits._

class VPackSmallintCodecSpec extends AnyFlatSpec with VPackCodecSpecTrait {

  it should "encode small integers 0, 1, ... 9 to 0x30-0x39" in {
    assertCodec(vpackCodec, VSmallint(0), hex"30")
    assertCodec(vpackCodec, VSmallint(1), hex"31")
    assertCodec(vpackCodec, VSmallint(2), hex"32")
    assertCodec(vpackCodec, VSmallint(3), hex"33")
    assertCodec(vpackCodec, VSmallint(4), hex"34")
    assertCodec(vpackCodec, VSmallint(5), hex"35")
    assertCodec(vpackCodec, VSmallint(6), hex"36")
    assertCodec(vpackCodec, VSmallint(7), hex"37")
    assertCodec(vpackCodec, VSmallint(8), hex"38")
    assertCodec(vpackCodec, VSmallint(9), hex"39")
  }

  it should "encode small negative integers -6, -5, ..., -1 to 0x3a-0x3f" in {
    assertCodec(vpackCodec, VSmallint(-6), hex"3a")
    assertCodec(vpackCodec, VSmallint(-5), hex"3b")
    assertCodec(vpackCodec, VSmallint(-4), hex"3c")
    assertCodec(vpackCodec, VSmallint(-3), hex"3d")
    assertCodec(vpackCodec, VSmallint(-2), hex"3e")
    assertCodec(vpackCodec, VSmallint(-1), hex"3f")
  }

  it should "roundtrip" in {
    forAll(genVSmallint) { v: VSmallint =>
      assertEncodeDecode(vpackCodec, v)
    }
  }

  it should "fail if head is not a smallint" in {
    assert(vpackCodec.decode(hex"00".bits).isFailure)
  }
}
