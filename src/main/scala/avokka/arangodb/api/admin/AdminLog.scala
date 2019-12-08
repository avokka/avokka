package avokka.arangodb.api.admin

import avokka.velocypack._
import scodec.Codec

/**
 * @param level A list of the log levels for all log entries.
 * @param lid a list of log entry identifiers. Each log message is uniquely identified by its @LIT{lid} and the identifiers are in ascending order.
 * @param text a list of the texts of all log entries
 * @param timestamp a list of the timestamps as seconds since 1970-01-01 for all log entries.
 * @param topic a list of the topics of all log entries
 * @param totalAmount the total amount of log entries before pagination.
 */
case class AdminLog
(
  level: List[Long],
  lid: List[Long],
  text: List[String],
  timestamp: List[Long],
  topic: List[String],
  totalAmount: Long,
)

object AdminLog {
  implicit val codec: Codec[AdminLog] = VPackRecord[AdminLog].codecWithDefaults
}
