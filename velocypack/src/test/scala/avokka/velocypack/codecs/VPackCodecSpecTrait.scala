package avokka.velocypack
package codecs

import com.arangodb.velocypack.VPackSlice
import org.scalatest.{Assertion, Assertions}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.{BitVector, ByteVector}
import scodec.{Codec, Decoder, Encoder}

trait VPackCodecSpecTrait extends ScalaCheckPropertyChecks with VPackArbitrary { self: Assertions =>

  def assertEncode[T](c: Encoder[T], v: T, b: ByteVector): Assertion = {
    assertResult(b)(c.encode(v).require.bytes)
  }

  def assertEncodePack[T](c: Encoder[T], v: T, slice: VPackSlice): Assertion = {
    val r = c.encode(v).require
    assert(slice.equals(new VPackSlice(r.toByteArray)))
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
