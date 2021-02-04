package avokka.velocypack

import org.scalatest.flatspec.AnyFlatSpec
import shapeless.{::, HNil}

class VPackGenericSpec extends AnyFlatSpec with VPackSpecTrait {
  import VPack._
  import VPackGenericSpec._

  type R = String :: Boolean :: HNil
  val requestE: VPackEncoder[R] = VPackGeneric.Encoder[R]
  val requestD: VPackDecoder[R] = VPackGeneric.Decoder[VPackResult, R]
  val requestsE = VPackGeneric.Encoder[R :: R :: HNil]
  val requestsD = VPackGeneric.Decoder[VPackResult, R :: R :: HNil]
  val compactE = VPackGeneric.Encoder[Int :: Boolean :: HNil]

  "hnil" should "encode to empty array" in {
    val c = VPackGeneric.Encoder[HNil]
    assertEnc(c, HNil, VArray.empty)
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

    assertDec(requestD, VArray(VString("a"), VTrue), "a" :: true :: HNil)
    assertDec(requestsD,
      VArray(VArray(VString("a"), VTrue), VArray(VString("b"), VFalse)),
      ("a" :: true :: HNil) :: ("b" :: false :: HNil) :: HNil
    )

    assert(requestD.run(VTrue).isLeft)
    assert(requestD.run(VArray(VTrue, VString("a"))).isLeft)
    assert(requestD.run(VArray(VString("a"))).isLeft)
  }

  "derive from case class" should "encode to array" in {
    val r = Rcc("a", true)
    assertEnc(rccEncoder, r, VArray(VString("a"), VTrue))
  }

  "derive from case class" should "decode from array" in {
    val r = Rcc("a", true)
    assertDec(rccDecoder, VArray(VString("a"), VTrue), r)

    val f = rccDecoder.run(VTrue)
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
