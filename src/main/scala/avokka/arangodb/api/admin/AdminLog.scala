package avokka.arangodb.api.admin

import avokka.arangodb._
import avokka.arangodb.api.Api
import avokka.velocypack._

object AdminLog { self =>

  /**
   * @param level A list of the log levels for all log entries.
   * @param lid a list of log entry identifiers. Each log message is uniquely identified by its @LIT{lid} and the identifiers are in ascending order.
   * @param text a list of the texts of all log entries
   * @param timestamp a list of the timestamps as seconds since 1970-01-01 for all log entries.
   * @param topic a list of the topics of all log entries
   * @param totalAmount the total amount of log entries before pagination.
   */
  case class Response
  (
    level: List[Long],
    lid: List[Long],
    text: List[String],
    timestamp: List[Long],
    topic: List[String],
    totalAmount: Long,
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Session, AdminLog.type, Response] = new Api.EmptyBody[Session, AdminLog.type] {
    override type Response = self.Response
    override def header(session: Session, command: AdminLog.type): Request.HeaderTrait = Request.Header(
      database = Database.systemName,
      requestType = RequestType.GET,
      request = "/_admin/log"
    )
  }
}
