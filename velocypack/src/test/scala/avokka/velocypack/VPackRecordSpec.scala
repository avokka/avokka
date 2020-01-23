package avokka.velocypack

import avokka.velocypack.VPackRecordSpec.VersionResponse
import org.scalatest.{FlatSpec, Matchers}

class VPackRecordSpec extends FlatSpec with Matchers with VPackSpecTrait {
  import VPack._
  import VPackRecordSpec._

  /*
  "generic codec" should "conform specs" in {

    val c = VPackRecord.codec[
      FieldType[Witness.`'test`.T, Boolean] ::
      FieldType[Witness.`'code`.T, Int] ::
      HNil,
      HNil
    ]()

    assertEncode(c, 'test ->> false :: 'code ->> 200 :: HNil,
      hex"0b 12 02 44636f6465 28c8 4474657374 19 03 0a"
    )
    assert(c.decode(hex"0b 0a 01 44636f6465 35 03".bits).isFailure)
    assertDecode(c, hex"0b 11 02 4474657374 1a 44636f6465 35 09 03",
      'test ->> true :: 'code ->> 5 :: HNil
    )
    assertDecode(c, hex"0b 11 02 44636f6465 35 4474657374 1a 09 03",
      'test ->> true :: 'code ->> 5 :: HNil
    )
  }
   */

  "case class codec" should "conform specs" in {
    assertDec(VersionResponseDecoder,
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2"))),
      VersionResponse("arango", "community", "3.5.2")
    )
    assertEnc(VersionResponseEncoder,
      VersionResponse("arango", "community", "3.5.2"),
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2")))
    )
    /*
    assertEnc(VersionResponseEncoderM,
      VersionResponse("arango", "community", "3.5.2"),
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2")))
    )
     */
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

object VPackRecordSpec {

  case class VersionResponse
  (
    server: String,
    license: String,
    version: String
  )

//  val VersionResponseEncoderM: VPackEncoder[VersionResponse] = VPackEncoder.derive

  val VersionResponseEncoder: VPackEncoder[VersionResponse] = VPackRecord[VersionResponse].encoder
  val VersionResponseDecoder: VPackDecoder[VersionResponse] = VPackRecord[VersionResponse].decoder

  case class TestDefault
  (
    a: Boolean,
    i: Int = 10
  )

  val TestDefaultEncoder: VPackEncoder[TestDefault] = VPackRecord[TestDefault].encoder
  val TestDefaultDecoder: VPackDecoder[TestDefault] = VPackRecord[TestDefault].decoderWithDefaults
}
