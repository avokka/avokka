package avokka.arangodb
package api

import types._

final case class CollectionUnload(
    name: CollectionName
)

object CollectionUnload {

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionUnload, CollectionInfo.Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionUnload] {
      override type Response = CollectionInfo.Response
      override def header(database: ArangoDatabase,
                          command: CollectionUnload): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.PUT,
          request = s"/_api/collection/${command.name}/unload",
        )
    }*/

}
