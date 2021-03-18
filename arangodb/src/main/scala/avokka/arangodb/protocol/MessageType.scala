package avokka.arangodb.protocol

import enumeratum._
import enumeratum.values._
import cats.Show

sealed abstract class MessageType(val value: Int, val show: String) extends IntEnumEntry

object MessageType extends IntEnum[MessageType] with VPackValueEnum[Int, MessageType] {

  case object Request extends MessageType(1,"request")
  case object ResponseFinal extends MessageType(2,"response-final")
  case object ResponseChunk extends MessageType(3,"response-chunk")
  case object Authentication extends MessageType(1000,"authentication")

  override val values = findValues

  implicit val show: Show[MessageType] = _.show
}
