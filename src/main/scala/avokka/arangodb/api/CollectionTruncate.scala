package avokka.arangodb.api

import avokka.arangodb.{Collection, Request, RequestType}
import avokka.velocypack._

object CollectionTruncate {

  implicit val api: Api.EmptyBody.Aux[Collection, CollectionTruncate.type, CollectionInfo.Response] = new Api.EmptyBody[Collection, CollectionTruncate.type] {
    override type Response = CollectionInfo.Response
    override def requestHeader(collection: Collection, command: CollectionTruncate.type): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/${collection.name}/truncate",
    )
  }

}
