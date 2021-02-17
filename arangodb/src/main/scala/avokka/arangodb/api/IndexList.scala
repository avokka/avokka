package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class IndexList(
                            indexes: List[Index.Response],
                            identifiers: Map[String, Index.Response]
                          )

object IndexList {

    implicit val decoder: VPackDecoder[IndexList] = VPackDecoder.gen

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, IndexList, Response] =
    new Api.EmptyBody[ArangoDatabase, IndexList] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: IndexList): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = "/_api/index",
          parameters = Map("collection" -> command.collection.repr)
        )
    }*/
}
