package avokka.velocypack

import java.time.Instant

import VPack._
import cats.syntax.show._
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.ByteVector

class VPackShowSpec extends FlatSpec with Matchers {

  "show" should "produce json" in {
    assertResult("\"a\"")(VString("a").show)
    assertResult("12.34")(VDouble(12.34).show)

    val dt = "2020-01-29T15:45:10Z"
    assertResult(s""""$dt"""")(VDate(Instant.parse(dt).toEpochMilli).show)

    assertResult("\"5010\"")(VBinary(ByteVector(0x50, 0x10)).show)

    assertResult("[\"a\",true,1]")(VArray(VString("a"), VTrue, VSmallint(1)).show)
    assertResult("{\"b\":true,\"a\":[0,1]}")(VObject(Map("b" -> VTrue, "a" -> VArray(VSmallint(0), VSmallint(1)))).show)

    assertResult("null")((VNull: VPack).show)
    assertResult("{}")((VObject.empty: VPack).show)
  }
}
