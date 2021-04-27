package avokka.velocypack
package circe

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues._
import _root_.io.circe.literal._

class VPackCirceSpec extends AnyFlatSpec with Matchers {

  "circe" should "encode to vpack" in {

    json"true".toVPack should be (VTrue)

    json"5".toVPack should be (VSmallint(5))

    json"500".toVPack should be (VLong(500))

    json"0.5".toVPack should be (VDouble(.5))

    json"null".toVPack should be (VNull)

    json""""s"""".toVPack should be (VString("s"))

    json"""
           {"a": true}
    """.toVPack should be (VObject("a" -> VTrue))

    json"""
           ["a", true]
    """.toVPack should be (VArray("a".toVPack, VTrue))

    json"""
           { "name":"John", "age":30, "car":null }
    """.toVPack should be (VObject("name" -> "John".toVPack, "age" -> 30.toVPack, "car" -> VNull))

  }

}

