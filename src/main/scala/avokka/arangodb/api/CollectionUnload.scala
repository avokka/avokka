package avokka.arangodb.api

import avokka.arangodb.{Collection, Request, RequestType}

object CollectionUnload {

  implicit val api: Api.EmptyBody.Aux[Collection, CollectionUnload.type, CollectionInfo.Response] = new Api.EmptyBody[Collection, CollectionUnload.type] {
    override type Response = CollectionInfo.Response
    override def requestHeader(collection: Collection, command: CollectionUnload.type): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/${collection.name}/unload",
    )
  }

}
