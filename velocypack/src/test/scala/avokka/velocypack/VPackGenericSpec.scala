package avokka.velocypack

import avokka.velocypack.codecs.VPackCodecSpecTrait
import org.scalatest._
import shapeless.{::, HNil}

class VPackGenericSpec extends FlatSpec with Matchers with VPackSpecTrait {
  import VPack._
  import VPackGenericSpec._

  type R = String :: Boolean :: HNil
  val requestE: VPackEncoder[R] = VPackGeneric.encoder[R]()
  val requestD: VPackDecoder[R] = VPackGeneric.decoder[R]
  val requestsE = VPackGeneric.encoder[R :: R :: HNil]()
  val requestsD = VPackGeneric.decoder[R :: R :: HNil]
  val compactE = VPackGeneric.encoder[Int :: Boolean :: HNil](true)

  "hnil" should "encode to empty array" in {
    val c = VPackGeneric.encoder[HNil]()
    assertEnc(c, HNil, VArrayEmpty)
  }

  "hlist encoders" should "encode to arrays" in {

    // compact
    assertEnc(compactE, 10 :: false :: HNil, VArray(VLong(10), VFalse))

    // indexed
    assertEnc(requestE, "a" :: true :: HNil, VArray(VString("a"), VTrue))

    // recurse
    assertEnc(requestsE,
      ("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil,
      VArray(VArray(VString("a"), VTrue), VArray(VString("b"), VFalse))
    )
  }

  "hlist decoders" should "decode from arrays" in {

    val f = requestD.decode(VTrue)
    assert(f.isLeft)

    assertDec(requestD, VArray(VString("a"), VTrue), "a" :: true :: HNil)
    assertDec(requestsD,
      VArray(VArray(VString("a"), VTrue), VArray(VString("b"), VFalse)),
      ("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil
    )
  }

  "derive from case class" should "encode to array" in {
    val r = Rcc("a", true)
    assertEnc(rccEncoder, r, VArray(VString("a"), VTrue))
  }

  "derive from case class" should "decode from array" in {
    val r = Rcc("a", true)
    assertDec(rccDecoder, VArray(VString("a"), VTrue), r)

    val f = rccDecoder.decode(VTrue)
    assert(f.isLeft)
  }
}

object VPackGenericSpec {

  case class Rcc
  (
    str: String,
    bool: Boolean
  )

  implicit val rccEncoder: VPackEncoder[Rcc] = VPackGeneric[Rcc].encoder
  implicit val rccDecoder: VPackDecoder[Rcc] = VPackGeneric[Rcc].decoder

}
