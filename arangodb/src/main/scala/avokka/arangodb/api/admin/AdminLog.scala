package avokka.arangodb
package api
package admin

import avokka.velocypack._
import _root_.enumeratum._

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
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  sealed trait Level extends EnumEntry
  object Level extends Enum[Level] {
    val values = findValues

    case object FATAL extends Level
    case object ERROR extends Level
    case object WARNING extends Level
    case object INFO extends Level
    case object DEBUG extends Level
    case object TRACE extends Level
    case object DEFAULT extends Level
  }

  sealed trait Topic extends EnumEntry
  object Topic extends Enum[Topic] {
    val values = findValues
    
    case object agencycomm extends Topic
    case object agencystore extends Topic
    case object agency extends Topic
    case object aql extends Topic
    case object arangosearch extends Topic
    case object authentication extends Topic
    case object authorization extends Topic
    case object backup extends Topic
    case object cache extends Topic
    case object clustercomm extends Topic
    case object cluster extends Topic
    case object collector extends Topic
    case object communication extends Topic
    case object compactor extends Topic
    case object config extends Topic
    case object crash extends Topic
    case object datafiles extends Topic
    case object development extends Topic
    case object dump extends Topic
    case object engines extends Topic
    case object flush extends Topic
    case object general extends Topic
    case object graphs extends Topic
    case object heartbeat extends Topic
    case object httpclient extends Topic
    case object libiresearch extends Topic
    case object maintenance extends Topic
    case object memory extends Topic
    case object mmap extends Topic
    case object performance extends Topic
    case object pregel extends Topic
    case object queries extends Topic
    case object replication extends Topic
    case object requests extends Topic
    case object restore extends Topic
    case object rocksdb extends Topic
    case object security extends Topic
    case object ssl extends Topic
    case object startup extends Topic
    case object statistics extends Topic
    case object supervision extends Topic
    case object syscall extends Topic
    case object threads extends Topic
    case object trx extends Topic
    case object ttl extends Topic
    case object v8 extends Topic
    case object validation extends Topic
    case object views extends Topic
  }

  type Levels = Map[Topic, Level]

/*  implicit val api: Api.EmptyBody.Aux[ArangoSession, AdminLog.type, Response] = new Api.EmptyBody[ArangoSession, AdminLog.type] {
    override type Response = self.Response
    override def header(session: ArangoSession, command: AdminLog.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = DatabaseName.system,
      requestType = RequestType.GET,
      request = "/_admin/log"
    )
  }*/
}
