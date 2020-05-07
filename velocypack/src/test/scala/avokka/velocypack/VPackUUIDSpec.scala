package avokka.velocypack

import java.util.UUID

import avokka.velocypack.VPack.{VBinary, VString}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector

class VPackUUIDSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "uuid" should "encode to bin" in {
    forAll { uuid: UUID =>
      val bin = ByteVector.fromLong(uuid.getMostSignificantBits) ++ ByteVector.fromLong(uuid.getLeastSignificantBits)
      assertEnc(VPackEncoder[UUID], uuid, VBinary(bin))
    }
  }

  "uuid" should "decode from bin or string" in {
    forAll { uuid: UUID =>
      assertDec(VPackDecoder[UUID], VBinary(ByteVector.fromUUID(uuid)), uuid)
      assertDec(VPackDecoder[UUID], VString(uuid.toString), uuid)
    }
  }

  "uuid" should "roundtrip" in {
    forAll { uuid: UUID =>
      assertRoundtrip(uuid)
    }
  }
}
