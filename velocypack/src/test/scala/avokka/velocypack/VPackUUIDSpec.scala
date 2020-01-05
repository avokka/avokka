package avokka.velocypack

import java.util.UUID

import avokka.velocypack.VPack.{VBinary, VString}
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.{BitVector, ByteVector}

class VPackUUIDSpec extends FlatSpec with Matchers with VPackSpecTrait {

  "uuid" should "encode to bin" in {
    val uuid = UUID.randomUUID()
    val bin = ByteVector.fromLong(uuid.getMostSignificantBits) ++ ByteVector.fromLong(uuid.getLeastSignificantBits)

    assertEnc(VPackEncoder[UUID], uuid, VBinary(bin))
  }

  "uuid" should "decode from bin or string" in {
    val uuid = UUID.randomUUID()
    assertDec(VPackDecoder[UUID], VString(uuid.toString), uuid)
    assertDec(VPackDecoder[UUID], VBinary(ByteVector.fromUUID(uuid)), uuid)
  }
}
