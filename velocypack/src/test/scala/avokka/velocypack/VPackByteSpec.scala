package avokka.velocypack

import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VPackByteSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "byte" should "fail with incoherent types" in {
    VString("test").as[Byte].left.value should be (a [VPackError.WrongType])
  }

  "byte" should "encode to smallint or long" in {
    assertCodec(0.toByte, VSmallint(0))
    assertCodec(100.toByte, VLong(100))
    assertRoundtrip(Byte.MinValue)
    assertRoundtrip(Byte.MaxValue)
  }

  "byte" should "fail when overflowing" in {
    VLong(1000).as[Byte].left.value should be (a [VPackError.Overflow])
  }

  "byte" should "decode double if valid" in {
    assertDecode(VDouble(5), 5.toByte)
    VDouble(5.1).as[Byte].left.value should be (a [VPackError.WrongType])
  }

  "byte" should "roundtrip" in {
    forAll { b: Byte =>
      assertRoundtrip(b)
    }
  }
}
