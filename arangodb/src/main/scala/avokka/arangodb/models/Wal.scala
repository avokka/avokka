package avokka.arangodb.models

import avokka.arangodb.types.DatabaseName
import avokka.velocypack.{VObject, VPackDecoder}

case class Wal
(
    tick: String,
    `type`: WalType,
    db: DatabaseName,
    cuid: Option[String] = None,
    data: VObject = VObject.empty
)

object Wal {
  implicit val decoder: VPackDecoder[Wal] = VPackDecoder.derived
}