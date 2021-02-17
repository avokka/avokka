package avokka.arangodb
package api

import avokka.velocypack._

/**
  * the storage engine the server is configured to use
  *
  * @param name will be mmfiles or rocksdb
  * @param supports what the engine supports
  */
case class Engine(
    name: String,
    supports: Engine.Supports
)

object Engine {

  final case class Supports(
      dfdb: Boolean,
      indexes: List[String],
  )
  object Supports {
    implicit val decoder: VPackDecoder[Supports] = VPackDecoder.gen
  }

  implicit val decoder: VPackDecoder[Engine] = VPackDecoder.gen

}
