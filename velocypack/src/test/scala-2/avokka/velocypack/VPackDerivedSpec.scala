package avokka.velocypack

import org.scalacheck._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VPackDerivedSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {
  import VPackDerivedSpec._

  "case class codec" should "conform specs" in {
    assertDec(VersionResponseDecoder,
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2"))),
      VersionResponse("arango", "community", "3.5.2")
    )
    assertEnc(VersionResponseEncoder,
      VersionResponse("arango", "community", "3.5.2"),
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2")))
    )

    forAll { v: VersionResponse =>
      assertRoundtrip(v)(VersionResponseEncoder, VersionResponseDecoder)
    }
  }

  "case class codec with defaults" should "conform specs" in {
    assertEnc(TestDefaultEncoder,
      TestDefault(false, 0),
      VObject(Map("a" -> VFalse, "i" -> VSmallint(0)))
    )
    assertDec(TestDefaultDecoder,
      VObject(Map("a" -> VFalse, "i" -> VSmallint(0))),
      TestDefault(false, 0),
    )
    assertDec(TestDefaultDecoder,
      VObject(Map("i" -> VSmallint(0), "a" -> VFalse)),
      TestDefault(false, 0),
    )
    assertDec(TestDefaultDecoder,
      VObject(Map("a" -> VFalse)),
      TestDefault(false),
    )
    assert(TestDefaultDecoder.decode(VObject(Map("i" -> VSmallint(0)))).isLeft)
  }

}

object VPackDerivedSpec {

  import Arbitrary._

  case class VersionResponse
  (
    server: String,
    license: String,
    version: String
  )

  implicit val arbitrayVR: Arbitrary[VersionResponse] = Arbitrary(Gen.resultOf(VersionResponse.tupled))

  val VersionResponseEncoder: VPackEncoder[VersionResponse] = VPackEncoder.derived
  val VersionResponseDecoder: VPackDecoder[VersionResponse] = VPackDecoder.derived

  case class TestDefault
  (
    a: Boolean,
    i: Int = 10
  )

  val TestDefaultEncoder: VPackEncoder[TestDefault] = VPackEncoder.derived
  val TestDefaultDecoder: VPackDecoder[TestDefault] = VPackDecoder.derived
}

