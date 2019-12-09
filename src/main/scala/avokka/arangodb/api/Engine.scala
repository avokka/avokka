package avokka.arangodb.api

import avokka.velocypack._
import scodec.Codec

/**
 * the storage engine the server is configured to use
 *
 * @param name will be mmfiles or rocksdb
 * @param supports what the engine supports
 */
case class Engine
(
  name: String,
  supports: Engine.Supports
)

object Engine {
  case class Supports
  (
    dfdb: Boolean,
    indexes: List[String],
  )
  object Supports {
    implicit val codec: Codec[Supports] = VPackRecord[Supports].codecWithDefaults
  }

  implicit val codec: Codec[Engine] = VPackRecord[Engine].codecWithDefaults
}