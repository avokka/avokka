package avokka.velocypack

import avokka.velocypack.VPackValue.{vpString, vpBool, vpInt, vpDouble, codec => vpCodec}
import com.arangodb.velocypack.VPack
import org.scalatest._
import scodec.Codec
import scodec.bits._

class VPackValueSpec extends FlatSpec with Matchers {
  val vpack = new VPack.Builder().build()

  "0x00" should "not be allowed in vpack values" in {
    assert(vpCodec.decode(hex"00".bits).isFailure)
  }

  def assertCodec[T](c: Codec[T], v: T, b: ByteVector) = {
    assertResult(b)(c.encode(v).require.bytes)
    assertResult(v)(c.decode(b.bits).require.value)
  }

  "empty string" should "encode to 0x40" in {
    assertCodec(vpString, "", hex"40")
  }

  "small strings" should "encode between 0x41 0xbe" in {
    assertCodec(vpString, "@", hex"4140")
    assertCodec(vpString, "@@", hex"424040")
    assertCodec(vpString, "€", hex"43e282ac")
    assertCodec(vpString, "aфᐃ\uD835\uDD6B", hex"4a61d184e19083f09d95ab")
  }

  "long strings" should "encode at 0xbf" in {
    val len = 300
    assertCodec(vpString, "@" * len,
      hex"bf" ++ ByteVector.fromLong(len, 8, ByteOrdering.LittleEndian) ++ ByteVector.fill(len)(0x40)
    )
  }

  "ints" should "encode to the most compact form" in {
    assertCodec(vpInt, 1, hex"31")
    assertCodec(vpInt, 16, hex"2810")
  }

  "double" should "encode at 0x1b" in {
    assertCodec(vpDouble, 1.2,
      hex"1b" ++ ByteVector.fromLong(java.lang.Double.doubleToRawLongBits(1.2), 8, ByteOrdering.LittleEndian)
    )
  }

  "boolean" should "encode to 0x19 or 0x1a" in {
    assertCodec(vpBool, false, hex"19")
    assertCodec(vpBool, true, hex"1a")
    assert(vpBool.decode(hex"18".bits).isFailure)
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
