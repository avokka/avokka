package avokka.arangodb.models

import enumeratum._
import enumeratum.values._

sealed abstract class CollectionType(val value: Int) extends IntEnumEntry

object CollectionType extends IntEnum[CollectionType] with VPackValueEnum[Int, CollectionType] {

  case object Unknown extends CollectionType(0)
  case object Document extends CollectionType(2)
  case object Edge extends CollectionType(3)

  val values = findValues
}
