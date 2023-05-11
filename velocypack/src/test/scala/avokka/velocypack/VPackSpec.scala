package avokka.velocypack

import java.time.Instant
import java.util.Date

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector
import org.scalatest.EitherValues._

class VPackSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks with VPackSpecTrait {

  "codec from vector" should "encode vectors" in {
    assertCodec(Vector(1,2,3), VArray(VSmallint(1), VSmallint(2), VSmallint(3)))

    forAll { (v: Vector[Long]) =>
      assertRoundtrip(v)
    }
  }

  "double / floats" should "encode to most compact form" in {
    assertCodec(0d, VSmallint(0))
    assertCodec(10d, VLong(10))
    assertCodec(0.0001d, VDouble(0.0001d))
    assertRoundtrip(Double.MinValue)
    assertRoundtrip(Double.MaxValue)

    forAll { (d: Double) =>
      assertRoundtrip(d)
    }

    assertCodec(0f, VSmallint(0))
    assertCodec(10f, VLong(10))
    assertCodec(0.0001f, VDouble(0.0001f))
    assertRoundtrip(Float.MinValue)
    assertRoundtrip(Float.MaxValue)

    forAll { (f: Float) =>
      assertRoundtrip(f)
    }
  }

  "map" should "encode to objects" in {
    assertCodec(Map("z" -> 1, "a" -> 2), VObject(Map("z" -> VSmallint(1), "a" -> VSmallint(2))))

    assertCodec(Map(Symbol("z") -> 1, Symbol("a") -> 2), VObject(Map("z" -> VSmallint(1), "a" -> VSmallint(2))))
  }

  "object syntax" should "allow simple object creation" in {
    VObject("a" -> true.toVPack, "b" -> 1.toVPack) should be (VObject(Map("a" -> VTrue, "b" -> VSmallint(1))))

    VObject(Symbol("a") -> true.toVPack) should be (VObject(Map("a" -> VTrue)))
  }

  "empty string" should "decode to empty bytevector" in {
    assertDec(VPackDecoder[ByteVector], VString(""), ByteVector.empty)
    assertDec(VPackDecoder[Array[Byte]], VString(""), Array.empty[Byte])
  }

  "date" should "encode and decode" in {
    val now = new Date
    val ins = now.toInstant
    assertCodec(now, VDate(now.getTime))
    assertDecode(VLong(now.getTime), now)

    // ISO-8601 vpack strings should decode to date
    assertDec(VPackDecoder[Date], VString(ins.toString), now)
    assertDec(VPackDecoder[Instant], VString(ins.toString), ins)
    // and fail if not ISO
    VString("test").as[Date].left.value should be (a [VPackError.Conversion])
    VString("test").as[Instant].left.value should be (a [VPackError.Conversion])

    forAll { (d: Date) =>
      assertRoundtrip(d)
      assertRoundtrip(d.toInstant)
    }

  }

  "bignumber" should "roundtrip" in {
    assertCodec(BigInt(1), VSmallint(1))
    assertCodec(BigInt(100L), VLong(100))

    forAll { (i: BigInt) =>
      assertRoundtrip(i)
    }
    assertDecode(VDouble(1), BigInt(1))

    assertCodec(BigDecimal(1), VSmallint(1))
    assertCodec(BigDecimal(100L), VLong(100))
    assertCodec(BigDecimal(0.01d), VDouble(0.01d))

    forAll { (i: BigDecimal) =>
      assertRoundtrip(i)
    }
  }

  "option" should "encode and decode" in {
    assertCodec(None: Option[String], VNull)
    assertCodec(Option("data"), VString("data"))
  }

  "bytevector" should "encode and decode" in {
    assertCodec(ByteVector(10,100), VBinary(ByteVector(10,100)))
    // decode hexadecimal strings
    assertDecode(VString("0x0506"), ByteVector(5,6))
    // fail if non hex
    VString("test").as[ByteVector].left.value should be (a [VPackError.Conversion])
    // fail if incoherent vpack type
    VLong(1).as[ByteVector].left.value should be (a [VPackError.WrongType])

  }
}
