package avokka.arangodb
package aql

import avokka.velocypack._

/**
  * aql string context bound value
  */
trait AqlBindVar {
  /**
    * @return vpack value
    */
  def value: VPack
}

object AqlBindVar {
  /**
    * implicit conversion to VPack
    * @param t value
    * @param e vpack encoder
    * @tparam T value type
    * @return bound var
    */
  implicit def from[T](t: T)(implicit e: VPackEncoder[T]): AqlBindVar = new AqlBindVar {
    override val value: VPack = e.encode(t)
  }
}
