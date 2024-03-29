package avokka.arangodb
package models
package admin

import avokka.velocypack._
import enumeratum._

object AdminLog { self =>

  /**
    * @param level A list of the log levels for all log entries.
    * @param lid a list of log entry identifiers. Each log message is uniquely identified by its @LIT{lid} and the identifiers are in ascending order.
    * @param text a list of the texts of all log entries
    * @param timestamp a list of the timestamps as seconds since 1970-01-01 for all log entries.
    * @param topic a list of the topics of all log entries
    * @param totalAmount the total amount of log entries before pagination.
    */
  final case class Response(
      level: List[Long],
      lid: List[Long],
      text: List[String],
      timestamp: List[Long],
      topic: List[String],
      totalAmount: Long,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.derived
  }

  sealed trait Level extends EnumEntry
  object Level extends Enum[Level] with VPackEnum[Level] {
    val values = findValues

    case object FATAL extends Level
    case object ERROR extends Level
    case object WARNING extends Level
    case object INFO extends Level
    case object DEBUG extends Level
    case object TRACE extends Level
    case object DEFAULT extends Level
  }

  type Levels = Map[String, Level]

/*  implicit val api: Api.EmptyBody.Aux[ArangoSession, AdminLog.type, Response] = new Api.EmptyBody[ArangoSession, AdminLog.type] {
    override type Response = self.Response
    override def header(session: ArangoSession, command: AdminLog.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = DatabaseName.system,
      requestType = RequestType.GET,
      request = "/_admin/log"
    )
  }*/
}
