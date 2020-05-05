package avokka.velocypack

import org.scalatest.{Assertion, Assertions}
import org.scalatest.EitherValues._

trait VPackSpecTrait { self: Assertions =>
  def assertEnc[T](e: VPackEncoder[T], t: T, v: VPack): Assertion = {
    assertResult(v)(e.encode(t))
  }
  def assertDec[T](d: VPackDecoder[T], v: VPack, t: T): Assertion = {
    assertResult(t)(d.decode(v).right.value)
  }
}
