package avokka.velocypack

import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VPackShortSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "short" should "fail for incoherent type" in {
    VString("test").as[Short].left.value should be(a[VPackError.WrongType])
  }

  "short" should "encode to smallint or long" in {
    assertCodec(0.toShort, VSmallint(0))
    assertCodec(100.toShort, VLong(100))
    assertCodec(1000.toShort, VLong(1000))
    assertRoundtrip(Short.MinValue)
    assertRoundtrip(Short.MaxValue)
  }

  "short" should "fail when overflowing" in {
    VLong(Long.MaxValue).as[Short].left.value should be(a[VPackError.Overflow])
  }

  "short" should "decode double if valid" in {
    assertDecode(VDouble(5), 5.toShort)
    VDouble(5.1).as[Short].left.value should be(a[VPackError.WrongType])
  }

  "short" should "roundtrip" in {
    forAll { (v: Short) =>
      assertRoundtrip(v)
    }
  }
}
