package avokka.velocypack

import org.scalatest.{Assertion, Assertions}

trait VPackSpecTrait { self: Assertions =>
  def assertEnc[T](e: VPackEncoder[T], t: T, v: VPack): Assertion = {
    assertResult(v)(e.encode(t))
  }
  def assertDec[T](d: VPackDecoder[T], v: VPack, t: T): Assertion = {
    val r = d.decode(v)
    assert(r.isRight)
    assertResult(t)(r.right.get)
  }
}
