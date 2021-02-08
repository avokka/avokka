package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class IndexList(
    collection: CollectionName
)

object IndexList { self =>

  final case class Response(
      indexes: List[Index.Response],
      identifiers: Map[String, Index.Response]
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, IndexList, Response] =
    new Api.EmptyBody[ArangoDatabase, IndexList] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: IndexList): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = "/_api/index",
          parameters = Map("collection" -> command.collection.repr)
        )
    }
}
