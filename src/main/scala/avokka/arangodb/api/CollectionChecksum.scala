package avokka.arangodb.api

import avokka.arangodb.{Collection, Request, RequestType}
import avokka.velocypack._

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
 * @param withRevisions include document revision ids in the checksum calculation
 * @param withData include document body data in the checksum calculation
 */
case class CollectionChecksum
(
  withRevisions: Boolean = false,
  withData: Boolean = false,
)
{
  def parameters = Map(
    "withRevisions" -> withRevisions.toString,
    "withData" -> withData.toString,
  )
}

object CollectionChecksum { self =>

  case class Response
  (
    checksum: String,
    revision: String,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.Aux[Collection, CollectionChecksum, Response] = new Api[Collection, CollectionChecksum] {
    override type Response = self.Response
    override def requestHeader(collection: Collection, command: CollectionChecksum): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/${collection.name}/checksum",
      parameters = command.parameters
    )
  }
}
