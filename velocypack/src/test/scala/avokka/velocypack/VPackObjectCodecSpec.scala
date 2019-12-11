package avokka.velocypack

import avokka.velocypack.codecs.VPackObjectCodec
import org.scalatest.{FlatSpec, Matchers}
import scodec.Codec
import scodec.bits._
import shapeless.labelled.FieldType
import shapeless.{::, HNil, Witness}
import shapeless.syntax.singleton._
/*
class VPackObjectCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {
  import VPackObjectCodecSpec._

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

  "case class codec" should "conform specs" in {
    assertDecode(VersionResponseCodec,
      hex"0b340346736572766572466172616e676f476c6963656e736549636f6d6d756e6974794776657273696f6e45332e352e32110323",
      VersionResponse("arango", "community", "3.5.2")
    )
  }

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
}

object VPackObjectCodecSpec {

  case class VersionResponse
  (
    server: String,
    license: String,
    version: String
  )

  val VersionResponseCodec: Codec[VersionResponse] = VPackRecord[VersionResponse].codec

  case class TestDefault
  (
    a: Boolean,
    i: Int = 10
  )

  val TestDefaultCodec: Codec[TestDefault] = VPackRecord[TestDefault].codecWithDefaults
}
*/