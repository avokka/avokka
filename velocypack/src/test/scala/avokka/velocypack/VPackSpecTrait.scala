package avokka.velocypack

import org.scalatest.{Assertion, Matchers}

trait VPackSpecTrait { self: Matchers =>
  def assertEnc[T](e: VPackEncoder[T], t: T, v: VPack) = {
    assertResult(v)(e.encode(t))
  }
  def assertDec[T](d: VPackDecoder[T], v: VPack, t: T) = {
    val r = d.decode(v)
    assert(r.isRight)
    assertResult(t)(r.right.get)
  }
}
