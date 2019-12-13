package avokka.velocypack

import avokka.velocypack.VPackRecordSpec.VersionResponse
import org.scalatest.{FlatSpec, Matchers}

class VPackRecordSpec extends FlatSpec with Matchers with VPackSpecTrait {
  import VPack._
  import VPackRecordSpec._

  /*
  "map codec" should "conform specs" in {

    val sint = VPackObjectCodec.mapOf(intCodec)
    assertEncodePack(sint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val mint = VPackObjectCodec.Unsorted.mapOf(intCodec)
    assertEncodePack(mint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val cint = VPackObjectCodec.Compact.mapOf(intCodec)
    assertCodec(cint, Map.empty[String, Int], hex"0a")
    assertCodec(cint, Map("a" -> 0, "b" -> 1, "c" -> 2), hex"14 0c 4161 30 4162 31 4163 32 03")

  }

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
  }
/*
  "case class codec with defaults" should "conform specs" in {
    assertEncode(TestDefaultCodec,
      TestDefault(false, 0),
      hex"0b0b024169304161190603",
    )
    assertDecode(TestDefaultCodec,
      hex"0b 0b 02 416930 416119 06 03",
      TestDefault(false, 0),
    )
    assertDecode(TestDefaultCodec,
      hex"0b 07 01 416119 03",
      TestDefault(false),
    )
  }

 */
}

object VPackRecordSpec {

  case class VersionResponse
  (
    server: String,
    license: String,
    version: String
  )

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
