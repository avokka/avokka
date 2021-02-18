package avokka.arangodb
package api

import avokka.velocypack._

final case class CursorDelete(
    id: String
)

object CursorDelete {

  implicit val decoder: VPackDecoder[CursorDelete] = VPackDecoder.gen

}
