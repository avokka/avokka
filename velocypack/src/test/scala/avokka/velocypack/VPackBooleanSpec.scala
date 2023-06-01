package avokka.velocypack

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.EitherValues._

class VPackBooleanSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with VPackSpecTrait {

  "boolean" should "fail if not vpack boolean" in {
    VString("test").as[Boolean].left.value should be (a [VPackError.WrongType])
  }

  "boolean" should "roundtrip" in {
    forAll { (b: Boolean) =>
      assertRoundtrip(b)
    }
  }
}
