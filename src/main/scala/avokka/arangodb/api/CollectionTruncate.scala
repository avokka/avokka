package avokka.arangodb
package api

object CollectionTruncate {

  implicit val api
    : Api.EmptyBody.Aux[ArangoCollection, CollectionTruncate.type, CollectionInfo.Response] =
    new Api.EmptyBody[ArangoCollection, CollectionTruncate.type] {
      override type Response = CollectionInfo.Response
      override def header(collection: ArangoCollection,
                          command: CollectionTruncate.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = collection.database.name,
        requestType = RequestType.PUT,
        request = s"/_api/collection/${collection.name}/truncate",
      )
    }

}
