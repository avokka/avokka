package avokka.arangodb.types

import avokka.velocypack._

case class DatabaseName(repr: String) {
  def isEmpty: Boolean = repr.isEmpty
}

object DatabaseName {
  implicit val encoder: VPackEncoder[DatabaseName] = VPackEncoder.stringEncoder.contramap(_.repr)
  implicit val decoder: VPackDecoder[DatabaseName] = VPackDecoder.stringDecoder.map(apply)
  val system: DatabaseName = DatabaseName("_system")
}