package avokka.velocypack

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import auto.encoder._
import auto.decoder._

class VPackAutoDerivedSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {
  import VPackAutoDerivedSpec._

  "case class codec" should "conform specs" in {
    assertDecode(
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2"))),
      VersionResponse("arango", "community", "3.5.2")
    )
    assertEncode(
      VersionResponse("arango", "community", "3.5.2"),
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2")))
    )
  }

  "case class codec with defaults" should "conform specs" in {
    assertEncode(
      TestDefault(false, 0),
      VObject(Map("a" -> VFalse, "i" -> VSmallint(0)))
    )
    assertDecode(
      VObject(Map("a" -> VFalse, "i" -> VSmallint(0))),
      TestDefault(false, 0),
    )
    assertDecode(
      VObject(Map("i" -> VSmallint(0), "a" -> VFalse)),
      TestDefault(false, 0),
    )
    assertDecode(
      VObject(Map("a" -> VFalse)),
      TestDefault(false),
    )
  }

  "mixing auto and implicit encoder" should "prefer implicit" in {
    assertEncode(
      Parent(Child("test")),
      VObject("child" -> VObject("value" -> VString("test")))
    )
    assertDecode(
      VObject("child" -> VString("test")),
      Parent(Child("test"))
    )
  }

}

object VPackAutoDerivedSpec {

  import Arbitrary._

  case class VersionResponse
  (
    server: String,
    license: String,
    version: String
  )

  implicit val arbitrayVR: Arbitrary[VersionResponse] = Arbitrary(Gen.resultOf(VersionResponse.tupled))

  case class TestDefault
  (
    a: Boolean,
    i: Int = 10
  )

  case class Child
  (
      v: String
  )
  object Child {
    // custom codec
    implicit val encoder: VPackEncoder[Child] = (t: Child) => VObject("value" -> t.v.toVPack)
    implicit val decoder: VPackDecoder[Child] = {
      case VString(s) => Right(Child(s))
      case _ => Left(VPackError.IllegalValue("decode a string instead"))
    }
  }

  case class Parent
  (
      child: Child
  )

}



