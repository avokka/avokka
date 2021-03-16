package avokka.arangodb
package api

import avokka.velocypack._
import enumeratum._

/**
  * the storage engine the server is configured to use
  *
  * @param name will be mmfiles or rocksdb
  * @param supports what the engine supports
  */
case class Engine(
    name: Engine.Name,
    supports: Engine.Supports
)

object Engine {

  sealed trait Name extends EnumEntry
  object Name extends Enum[Name] with VPackEnum[Name] {
    val values = findValues
    case object mmfiles extends Name
    case object rocksdb extends Name
  }

  final case class Supports(
      dfdb: Boolean,
      indexes: List[String],
  )
  object Supports {
    implicit val decoder: VPackDecoder[Supports] = VPackDecoder.gen
  }

  implicit val decoder: VPackDecoder[Engine] = VPackDecoder.gen

}
