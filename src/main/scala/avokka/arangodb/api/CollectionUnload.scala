package avokka.arangodb
package api

object CollectionUnload {

  implicit val api: Api.EmptyBody.Aux[ArangoCollection, CollectionUnload.type, CollectionInfo.Response] =
    new Api.EmptyBody[ArangoCollection, CollectionUnload.type] {
      override type Response = CollectionInfo.Response
      override def header(collection: ArangoCollection, command: CollectionUnload.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.PUT,
        request = s"/_api/collection/${collection.name}/unload",
      )
    }

}
