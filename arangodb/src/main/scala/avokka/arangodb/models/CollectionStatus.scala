package avokka.arangodb.models

import enumeratum._
import enumeratum.values._

sealed abstract class CollectionStatus(val value: Int) extends IntEnumEntry

object CollectionStatus extends IntEnum[CollectionStatus] with VPackValueEnum[Int, CollectionStatus] {

  case object Unknown extends CollectionStatus(0)
  case object Loaded extends CollectionStatus(3)
  case object Deleted extends CollectionStatus(5)

  override val values = findValues
}
