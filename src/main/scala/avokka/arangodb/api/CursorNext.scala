package avokka.arangodb
package api

import avokka.velocypack._

case class CursorNext[T](
    id: String
)

object CursorNext { self =>
  implicit def api[T: VPackDecoder]: Api.EmptyBody.Aux[Database, CursorNext[T], Cursor.Response[T]] =
    new Api.EmptyBody[Database, CursorNext[T]] {
      override type Response = Cursor.Response[T]
      override def header(database: Database, command: CursorNext[T]): Request.HeaderTrait =
        Request.Header(
          database = database.name,
          requestType = RequestType.PUT,
          request = s"/_api/cursor/${command.id}"
        )
    }
}