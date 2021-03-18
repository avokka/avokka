package avokka.arangodb.models

import enumeratum._
import enumeratum.values._

sealed abstract class CollectionStatus(val value: Int) extends IntEnumEntry

object CollectionStatus extends IntEnum[CollectionStatus] with VPackValueEnum[Int, CollectionStatus] {

  case object Unknown extends CollectionStatus(0)
  case object NewBorn extends CollectionStatus(1)
  case object Unloaded extends CollectionStatus(2)
  case object Loaded extends CollectionStatus(3)
  case object Unloading extends CollectionStatus(4)
  case object Deleted extends CollectionStatus(5)
  case object Loading extends CollectionStatus(6)

  override val values = findValues
}
