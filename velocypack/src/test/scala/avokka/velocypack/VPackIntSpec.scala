package avokka.velocypack

import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VPackIntSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "int" should "fail for incoherent type" in {
    VString("test").as[Int].left.value should be (a [VPackError.WrongType])
  }

  "int" should "encode to smallint or long" in {
    assertCodec(0, VSmallint(0))
    assertCodec(100, VLong(100))
    assertCodec(1000, VLong(1000))
    assertRoundtrip(Int.MinValue)
    assertRoundtrip(Int.MaxValue)
  }

  "int" should "fail when overflowing" in {
    VLong(Long.MaxValue).as[Int].left.value should be (a [VPackError.Overflow])
  }

  "int" should "decode double if valid" in {
    assertDecode(VDouble(5), 5.toInt)
    VDouble(5.1).as[Int].left.value should be (a [VPackError.WrongType])
  }

  "int" should "roundtrip" in {
    forAll { (v: Int) =>
      assertRoundtrip(v)
    }
  }
}
