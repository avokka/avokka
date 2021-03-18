package avokka.arangodb.protocol

import enumeratum._
import enumeratum.values._
import cats.Show

sealed abstract class RequestType(val value: Int) extends IntEnumEntry

object RequestType extends IntEnum[RequestType] with VPackValueEnum[Int, RequestType] {

  case object DELETE extends RequestType(0)
  case object GET extends RequestType(1)
  case object POST extends RequestType(2)
  case object PUT extends RequestType(3)
  case object HEAD extends RequestType(4)
  case object PATCH extends RequestType(5)
  case object OPTIONS extends RequestType(6)

  override val values = findValues

  implicit val show: Show[RequestType] = Show.fromToString
}
