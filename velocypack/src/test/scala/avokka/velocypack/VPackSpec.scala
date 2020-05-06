package avokka.velocypack

import VPack._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector

class VPackSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks with VPackSpecTrait {

  "codec from vector" should "encode vectors" in {
    assertCodec(Vector(1,2,3), VArray(VSmallint(1), VSmallint(2), VSmallint(3)))
  }

  "double / floats" should "encode to most compact form" in {
    assertCodec(0d, VSmallint(0))
    assertCodec(10d, VLong(10))
    assertCodec(0.0001d, VDouble(0.0001d))

    assertCodec(0f, VSmallint(0))
    assertCodec(10f, VLong(10))
    assertCodec(0.0001f, VDouble(0.0001f))

    forAll { f: Float =>
      assertRoundtrip(f)
    }
    forAll { d: Double =>
      assertRoundtrip(d)
    }
  }

  "map" should "encode to objects" in {
    assertCodec(Map("z" -> 1, "a" -> 2), VObject(Map("z" -> VSmallint(1), "a" -> VSmallint(2))))
  }

  "object syntax" should "allow simple object creation" in {
    VObject("a" -> true.toVPack, "b" -> 1.toVPack) should be (VObject(Map("a" -> VTrue, "b" -> VSmallint(1))))
  }

  "empty string" should "decode to empty bytevector" in {
    assertDec(VPackDecoder[ByteVector], VString(""), ByteVector.empty)
    assertDec(VPackDecoder[Array[Byte]], VString(""), Array.empty[Byte])
  }

  "byte" should "encode to smallint or long" in {
    assertCodec(0.toByte, VSmallint(0))
    assertCodec(100.toByte, VLong(100))

    VPackDecoder[Byte].decode(VLong(1000)).left.value should be (a [VPackError.Overflow])

    forAll { b: Byte =>
      assertRoundtrip(b)
    }
  }

  "int" should "encode to smallint or long" in {
    assertCodec(0, VSmallint(0))
    assertCodec(100, VLong(100))
    assertCodec(1000, VLong(1000))

    forAll { i: Int =>
      assertRoundtrip(i)
    }
  }

  "long" should "encode to smallint or long" in {
    assertCodec(0L, VSmallint(0))
    assertCodec(100L, VLong(100))
    assertCodec(1000L, VLong(1000))

    forAll { l: Long =>
      assertRoundtrip(l)
    }
  }
}
