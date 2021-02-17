package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class CollectionCount
(
  name: CollectionName,
)

object CollectionCount { self =>

  final case class Response(
      count: Long,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionCount, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionCount] {
      override type Response = self.Response

      override def header(database: ArangoDatabase, command: CollectionCount): ArangoRequest.Header = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${command.name}/count",
      )
    }*/
}
