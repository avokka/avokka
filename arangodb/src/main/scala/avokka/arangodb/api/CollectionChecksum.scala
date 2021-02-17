package avokka.arangodb
package api

import avokka.velocypack._
import types._

/**
  * Will calculate a checksum of the meta-data (keys and optionally revision ids) and
  * optionally the document data in the collection.
  *
  * The checksum can be used to compare if two collections on different ArangoDB
  * instances contain the same contents. The current revision of the collection is
  * returned too so one can make sure the checksums are calculated for the same
  * state of data.
  *
  * By default, the checksum will only be calculated on the _key system attribute
  * of the documents contained in the collection. For edge collections, the system
  * attributes _from and _to will also be included in the calculation.
  *
  * @param name          collection name
  * @param withRevisions include document revision ids in the checksum calculation
  * @param withData      include document body data in the checksum calculation
  */
final case class CollectionChecksum(
    name: CollectionName,
    withRevisions: Boolean = false,
    withData: Boolean = false,
) {
  def parameters = Map(
    "withRevisions" -> withRevisions.toString,
    "withData" -> withData.toString,
  )
}

object CollectionChecksum { self =>

  final case class Response(
      checksum: String,
      revision: String,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, CollectionChecksum, Response] =
    new Api.EmptyBody[ArangoDatabase, CollectionChecksum] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: CollectionChecksum): ArangoRequest.HeaderTrait = ArangoRequest.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/${command.name}/checksum",
        parameters = command.parameters
      )
    }*/
}
