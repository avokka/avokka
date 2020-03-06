package avokka.arangodb
package api

case class CollectionTruncate(
    name: CollectionName
)

object CollectionTruncate {

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionTruncate, CollectionInfo.Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionTruncate] {
      override type Response = CollectionInfo.Response
      override def header(database: ArangoDatabase,
                          command: CollectionTruncate): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.PUT,
          request = s"/_api/collection/${command.name}/truncate",
        )
    }

}
