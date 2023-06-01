package avokka.velocypack

import java.util.UUID

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector
// import cats.instances.either._
import org.scalatest.EitherValues._

class VPackUUIDSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "uuid" should "encode to bin" in {
    forAll { (uuid: UUID) =>
      val bin = ByteVector.fromLong(uuid.getMostSignificantBits) ++ ByteVector.fromLong(uuid.getLeastSignificantBits)
      assertEnc(VPackEncoder[UUID], uuid, VBinary(bin))
    }
  }

  "uuid" should "decode from bin or string" in {
    forAll { (uuid: UUID) =>
      assertDec(VPackDecoder[UUID], VBinary(ByteVector.fromUUID(uuid)), uuid)
      assertDec(VPackDecoder[UUID], VString(uuid.toString), uuid)
    }

    VPackDecoder[UUID].decode(VString("000")).left.value should be (a [VPackError.Conversion])
  }

  "uuid" should "roundtrip" in {
    forAll { (uuid: UUID) =>
      assertRoundtrip(uuid)
    }
  }

  "uuid" should "fail with incoherent type" in {
    VDate(0).as[UUID].left.value should be (a [VPackError.WrongType])
  }
}
