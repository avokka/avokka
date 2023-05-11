package avokka.velocypack

import java.time.Instant

import cats.syntax.show._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector

class VPackShowSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks with ScalaCheckPropertyChecks with VPackArbitrary {

  val examples = Table(
    ("vpack", "json"),

    (VBinary(ByteVector(0x50, 0x10)), "\"5010\""),

    (VArray(VString("a"), VTrue, VSmallint(1)), "[\"a\",true,1]"),
    (VObject(Map("b" -> VTrue, "a" -> VArray(VSmallint(0), VSmallint(1)))), "{\"b\":true,\"a\":[0,1]}"),

    (VNull,         "null"),
    (VArray.empty,  "[]"),
    (VObject.empty, "{}"),
  )

  "examples" should "show as json" in {
    forAll(examples) { (v: VPack, json: String) =>
      v.show should be (json)
    }
  }

  "string" should "show in quotes" in {
    forAll(genVString) { (v: VString) =>
      v.show should be (s""""${v.value}"""")
    }
  }

  "double" should "show as string" in {
    forAll(genVDouble) { (v: VDouble) =>
      v.show should be (v.value.toString)
    }
  }

  "date" should "show as" in {
    forAll(genVDate) { (v: VDate) =>
      val dt = Instant.ofEpochMilli(v.value)
      v.show should be (s""""$dt"""")
    }
  }

}
