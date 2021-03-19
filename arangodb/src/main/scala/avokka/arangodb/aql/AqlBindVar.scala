package avokka.arangodb
package aql

import avokka.arangodb.types.CollectionName
import avokka.velocypack._

/**
  * aql string context bound value
  */
trait AqlBindVar {
  /** velocypack bound value */
  def value: VPack

  /** parameter name prefix */
  def prefix: String
}

object AqlBindVar {

  /**
    * collection name are prefixed with extra @
    * @param c collection name
    * @return bound var
    */
  implicit def collection(c: CollectionName): AqlBindVar = new AqlBindVar {
    override def value: VPack = VString(c.repr)
    override def prefix: String = "@_"
  }

  /**
    * implicit conversion to VPack
    * @param t value
    * @param e vpack encoder
    * @tparam T value type
    * @return bound var
    */
  implicit def from[T](t: T)(implicit e: VPackEncoder[T]): AqlBindVar = new AqlBindVar {
    override val value: VPack = e.encode(t)
    override val prefix: String = "_"
  }
}
