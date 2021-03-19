package avokka.velocypack

import org.scalatest.{Assertion, Assertions}
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers

trait VPackSpecTrait extends Matchers { self: Assertions =>

  def assertEnc[T](e: VPackEncoder[T], t: T, v: VPack): Assertion = {
    e.encode(t) should be (v)
  }

  def assertDec[T](d: VPackDecoder[T], v: VPack, t: T): Assertion = {
    d.decode(v).value should be (t)
  }

  def assertEncode[T](t: T, v: VPack)(implicit e: VPackEncoder[T]): Assertion = {
    assertEnc(e, t, v)
  }

  def assertDecode[T](v: VPack, t: T)(implicit d: VPackDecoder[T]): Assertion = {
    assertDec(d, v, t)
  }

  def assertCodec[T](t: T, v: VPack)(implicit e: VPackEncoder[T], d: VPackDecoder[T]): Assertion = {
    assertEnc(e, t, v)
    assertDec(d, v, t)
  }

  def assertRoundtrip[T](t: T, echo: Boolean = false)(implicit e: VPackEncoder[T], d: VPackDecoder[T]): Assertion = {
    // vpack value roundtrip
    if (echo) {
      println(t)
      println(e.encode(t))
    }
    d.decode(e.encode(t)).value should be (t)
    // bits roundtrip
    if (echo) {
      println(e.bits(t).value)
    }
    d.decodeBits(e.bits(t).value).value.value should be (t)
  }
}
