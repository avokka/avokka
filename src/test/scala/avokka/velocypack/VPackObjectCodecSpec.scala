package avokka.velocypack

import avokka.velocypack.VPackValue.vpInt
import avokka.velocypack.codecs.VPackObjectCodec
import com.arangodb.velocypack.VPackSlice
import org.scalatest.{FlatSpec, Matchers}
import scodec.Codec
import scodec.bits._

class VPackObjectCodecSpec extends FlatSpec with Matchers {

  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def assertEncode[T](c: Codec[T], v: T, b: ByteVector) = {
    assertResult(b)(c.encode(v).require.bytes)
  }
  def assertEncodePack[T](c: Codec[T], v: T, json: String) = {
    val r = c.encode(v).require
    assertResult(json)(toSlice(r).toString)
  }

  def assertDecode[T](c: Codec[T], b: ByteVector, v: T) = {
    assertResult(v)(c.decode(b.bits).require.value)
  }

  def assertCodec[T](c: Codec[T], v: T, b: ByteVector) = {
    assertEncode(c, v, b)
    assertDecode(c, b, v)
  }

  "map codec" should "conform specs" in {

    val sint = VPackObjectCodec.mapOf(vpInt)
    assertEncodePack(sint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val mint = VPackObjectCodec.Unsorted.mapOf(vpInt)
    assertEncodePack(mint, Map("z" -> 1, "a" -> 2), """{"z":1,"a":2}""")

    val cint = VPackObjectCodec.Compact.mapOf(vpInt)
    assertCodec(cint, Map.empty[String, Int], hex"0a")
    assertCodec(cint, Map("a" -> 0, "b" -> 1, "c" -> 2), hex"14 0c 4161 30 4162 31 4163 32 03")


  }

}
