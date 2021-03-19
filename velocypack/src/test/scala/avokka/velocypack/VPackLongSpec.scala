package avokka.velocypack

import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VPackLongSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "long" should "fail for incoherent type" in {
    VString("test").as[Long].left.value should be(a[VPackError.WrongType])
  }

  "long" should "encode to smallint or long" in {
    assertCodec(0L, VSmallint(0))
    assertCodec(100L, VLong(100))
    assertCodec(1000L, VLong(1000))
    assertRoundtrip(Long.MinValue)
    assertRoundtrip(Long.MaxValue)
  }

  "long" should "decode double if valid" in {
    assertDecode(VDouble(5), 5L)
    VDouble(5.1).as[Long].left.value should be(a[VPackError.WrongType])
  }

  "long" should "roundtrip" in {
    forAll { v: Long =>
      assertRoundtrip(v)
    }
  }
}
