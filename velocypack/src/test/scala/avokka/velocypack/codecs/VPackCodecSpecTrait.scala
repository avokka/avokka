package avokka.velocypack.codecs

import com.arangodb.velocypack.VPackSlice
import org.scalatest.{Assertion, Assertions}
import scodec.bits.{BitVector, ByteVector}
import scodec.{Codec, Decoder, Encoder}

trait VPackCodecSpecTrait { self: Assertions =>

  val vpack = new com.arangodb.velocypack.VPack.Builder().build()

  def toSlice(bits: BitVector) = new VPackSlice(bits.toByteArray)

  def assertEncode[T](c: Encoder[T], v: T, b: ByteVector): Assertion = {
    assertResult(b)(c.encode(v).require.bytes)
  }

  def assertEncodePack[T](c: Encoder[T], v: T, json: String): Assertion = {
    val r = c.encode(v).require
    assertResult(json)(toSlice(r).toString)
  }

  def assertDecode[T](c: Decoder[T], b: ByteVector, v: T): Assertion = {
    assertResult(v)(c.decode(b.bits).require.value)
  }

  def assertCodec[T](c: Codec[T], v: T, b: ByteVector): Assertion = {
    assertEncode(c, v, b)
    assertDecode(c, b, v)
  }

  def assertEncodeDecode[T](c: Codec[T], v: T): Assertion = {
    val r = c.encode(v)
    assert(r.isSuccessful)
    val b = c.decode(r.require)
    assert(b.isSuccessful)
    assertResult(v)(b.require.value)
  }
}
