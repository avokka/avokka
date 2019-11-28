package avokka.velocypack

import avokka.velocypack.VPackValue.vpInt
import avokka.velocypack.codecs.VPackGenericCodec.deriveFor
import avokka.velocypack.codecs.{VPackGenericCodec, VPackObjectCodec}
import com.arangodb.velocypack.VPackSlice
import org.scalatest.{FlatSpec, Matchers}
import scodec.Codec
import scodec.bits._
import shapeless.HNil
import shapeless.syntax.singleton._

class VPackObjectCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {
  import VPackObjectCodecSpec._

  "map codec" should "conform specs" in {

    val sint = VPackObjectCodec.mapOf(vpInt)
    assertEncodePack(sint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val mint = VPackObjectCodec.Unsorted.mapOf(vpInt)
    assertEncodePack(mint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val cint = VPackObjectCodec.Compact.mapOf(vpInt)
    assertCodec(cint, Map.empty[String, Int], hex"0a")
    assertCodec(cint, Map("a" -> 0, "b" -> 1, "c" -> 2), hex"14 0c 4161 30 4162 31 4163 32 03")

  }

  "generic codec" should "conform specs" in {

    val c = VPackGenericCodec.codec(
      'test ->> VPackValue.vpBool ::
      'code ->> VPackValue.vpInt ::
      HNil
    )

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
    assertDecode(VersionResponse.codec,
      hex"0b340346736572766572466172616e676f476c6963656e736549636f6d6d756e6974794776657273696f6e45332e352e32110323",
      VersionResponse("arango", "community", "3.5.2")
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

  object VersionResponse {
    val codec: Codec[VersionResponse] = VPackGenericCodec.deriveFor[VersionResponse](
      'server ->> VPackValue.vpString ::
      'license ->> VPackValue.vpString ::
      'version ->> VPackValue.vpString ::
      HNil
    )
  }
}
