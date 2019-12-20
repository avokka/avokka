package avokka.arangodb
package api

object CollectionTruncate {

  implicit val api
    : Api.EmptyBody.Aux[Collection, CollectionTruncate.type, CollectionInfo.Response] =
    new Api.EmptyBody[Collection, CollectionTruncate.type] {
      override type Response = CollectionInfo.Response
      override def header(collection: Collection,
                          command: CollectionTruncate.type): Request.HeaderTrait = Request.Header(
        database = collection.database.name,
        requestType = RequestType.PUT,
        request = s"/_api/collection/${collection.name}/truncate",
      )
    }

}
