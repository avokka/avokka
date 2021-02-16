package avokka.velocypack

import java.time.Instant
import java.util.Date

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector

class VPackSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks with VPackSpecTrait {

  "codec from vector" should "encode vectors" in {
    assertCodec(Vector(1,2,3), VArray(VSmallint(1), VSmallint(2), VSmallint(3)))

    forAll { v: Vector[Long] =>
      assertRoundtrip(v)
    }
  }

  "double / floats" should "encode to most compact form" in {
    assertCodec(0d, VSmallint(0))
    assertCodec(10d, VLong(10))
    assertCodec(0.0001d, VDouble(0.0001d))
    assertRoundtrip(Double.MinValue)
    assertRoundtrip(Double.MaxValue)

    forAll { d: Double =>
      assertRoundtrip(d)
    }

    assertCodec(0f, VSmallint(0))
    assertCodec(10f, VLong(10))
    assertCodec(0.0001f, VDouble(0.0001f))
    assertRoundtrip(Float.MinValue)
    assertRoundtrip(Float.MaxValue)

    forAll { f: Float =>
      assertRoundtrip(f)
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
    assertRoundtrip(Byte.MinValue)
    assertRoundtrip(Byte.MaxValue)

    VPackDecoder[Byte].decode(VLong(1000)).left.value should be (a [VPackError.Overflow])

    forAll { b: Byte =>
      assertRoundtrip(b)
    }
  }

  "int" should "encode to smallint or long" in {
    assertCodec(0, VSmallint(0))
    assertCodec(100, VLong(100))
    assertCodec(1000, VLong(1000))
    assertRoundtrip(Int.MinValue)
    assertRoundtrip(Int.MaxValue)

    forAll { i: Int =>
      assertRoundtrip(i)
    }
  }

  "long" should "encode to smallint or long" in {
    assertCodec(0L, VSmallint(0))
    assertCodec(100L, VLong(100))
    assertCodec(1000L, VLong(1000))
    assertRoundtrip(Long.MinValue)
    assertRoundtrip(Long.MaxValue)

    forAll { l: Long =>
      assertRoundtrip(l)
    }
  }

  "date" should "encode and decode" in {
    val now = new Date
    val ins = now.toInstant
    assertCodec(now, VDate(now.getTime))

    // ISO-8601 vpack strings should decode to date
    assertDec(VPackDecoder[Date], VString(ins.toString), now)
    assertDec(VPackDecoder[Instant], VString(ins.toString), ins)

    forAll { d: Date =>
      assertRoundtrip(d)
      assertRoundtrip(d.toInstant)
    }

  }

  "boolean" should "roundtrip" in {
    forAll { b: Boolean =>
      assertRoundtrip(b)
    }
  }

  "bignumber" should "roundtrip" in {
    assertCodec(BigInt(1), VSmallint(1))
    assertCodec(BigInt(100L), VLong(100))

    forAll { i: BigInt =>
      assertRoundtrip(i)
    }

    assertCodec(BigDecimal(1), VSmallint(1))
    assertCodec(BigDecimal(100L), VLong(100))
    assertCodec(BigDecimal(0.01d), VDouble(0.01d))

    forAll { i: BigDecimal =>
      assertRoundtrip(i)
    }
  }
}
