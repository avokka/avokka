package avokka.velocypack.codecs

import avokka.velocypack.VPack._
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits._

class VPackObjectCodecSpec extends FlatSpec with Matchers with VPackCodecSpecTrait {

  "object codec" should "conform specs" in {

    assertEncodePack(vpackCodec, VObject(Map("z" -> VSmallint(1), "a" -> VSmallint(2))), """{"z":1,"a":2}""")

    assertCodec(VPackObjectCodec.codecCompact, VObject(Map.empty), hex"0a")
    assertCodec(VPackObjectCodec.codecCompact,
      VObject(Map("a" -> VSmallint(0), "b" -> VSmallint(1), "c" -> VSmallint(2))),
      hex"14 0c 4161 30 4162 31 4163 32 03"
    )

    assertEncode(VPackObjectCodec.codecSorted,
      VObject(Map("test" -> VFalse, "code" -> VLong(200))),
      hex"0b 12 02 4474657374 19 44636f6465 28c8 09 03"
    )
    assertDecode(vpackCodec, hex"0b 11 02 4474657374 1a 44636f6465 35 09 03",
      VObject(Map("test" -> VTrue, "code" -> VSmallint(5.toByte)))
    )
    assertDecode(vpackCodec, hex"0b 11 02 44636f6465 35 4474657374 1a 09 03",
      VObject(Map("test" -> VTrue, "code" -> VSmallint(5.toByte)))
    )

    assertDecode(vpackCodec,
      hex"0b340346736572766572466172616e676f476c6963656e736549636f6d6d756e6974794776657273696f6e45332e352e32110323",
      VObject(Map("server" -> VString("arango"), "license" -> VString("community"), "version"-> VString("3.5.2")))
    )
  }

}
