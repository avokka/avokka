package avokka.velocypack

import avokka.velocypack.VPackValue.vpString
import com.arangodb.velocypack.VPack
import org.scalatest._
import scodec.bits._

class VPackValueSpec extends FlatSpec with Matchers {
  val vpack = new VPack.Builder().build()

  "empty string" should "encode to 0x40" in {
    assertResult(hex"40")(vpString.encode("").require.bytes)
  }

  "small strings" should "encode between 0x41 0xbe" in {
    assertResult(hex"4140")(vpString.encode("@").require.bytes)
    assertResult(hex"424040")(vpString.encode("@@").require.bytes)
    assertResult(hex"43e282ac")(vpString.encode("€").require.bytes)
    assertResult(hex"4a61d184e19083f09d95ab")(vpString.encode("aфᐃ\uD835\uDD6B").require.bytes)
  }

  "long strings" should "encode at 0xbf" in {
    val len = 300
    assertResult(
      hex"bf" ++ ByteVector.fromLong(len, 8, ByteOrdering.LittleEndian) ++ ByteVector.fill(len)(0x40)
    )(vpString.encode("@" * len).require.bytes)
  }

  /*
  for {
    i <- -6 to 9
  } for {
    e <- VPackSmallInt.encoder.encode(i.toByte)
    ed <- codec.decode(e)
    d = vpack.deserialize(new VPackSlice(e.toByteArray), classOf[Int]): Int
  } yield println(e, ed, d)
*/
}
