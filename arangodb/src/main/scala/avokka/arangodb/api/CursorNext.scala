package avokka.arangodb
package api

import avokka.velocypack._

case class CursorNext[T](
    id: String
)

object CursorNext { self =>
  implicit def api[T: VPackDecoder]: Api.EmptyBody.Aux[ArangoDatabase, CursorNext[T], Cursor.Response[T]] =
    new Api.EmptyBody[ArangoDatabase, CursorNext[T]] {
      override type Response = Cursor.Response[T]
      override def header(database: ArangoDatabase, command: CursorNext[T]): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.PUT,
          request = s"/_api/cursor/${command.id}"
        )
    }
}