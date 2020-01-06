package avokka.velocypack

import avokka.velocypack.VPack._
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.ByteVector

class VPackSpec extends FlatSpec with Matchers with VPackSpecTrait {

  "codec from vector" should "encode vectors" in {
    val venc = VPackEncoder[Vector[Int]]
    assertEnc(venc, Vector(1,2,3), VArray(VSmallint(1), VSmallint(2), VSmallint(3)))

    val vdec = VPackDecoder[Vector[Int]]
    assertDec(vdec, VArray(VSmallint(1), VSmallint(2), VSmallint(3)), Vector(1,2,3))
  }

  "double" should "encode to most compact form" in {
    assertEnc(VPackEncoder[Double], 0d, VSmallint(0))
    assertEnc(VPackEncoder[Double], 10d, VLong(10))
  }

  "map" should "encode to objects" in {
    val sint = VPackEncoder[Map[String, Int]]
    assertEnc(sint, Map("z" -> 1, "a" -> 2), VObject(Map("z" -> VSmallint(1), "a" -> VSmallint(2))))
  }

  "object syntax" should "allow simple object creation" in {
    assertResult(VObject(Map("a" -> VTrue, "b" -> VSmallint(1))))(VObject("a" -> true.toVPack, "b" -> 1.toVPack))
  }

  "empty string" should "decode to empty bytevector" in {
    assertDec(VPackDecoder[ByteVector], VString(""), ByteVector.empty)
  }
}
