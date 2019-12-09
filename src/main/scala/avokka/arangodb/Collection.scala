package avokka.arangodb

import avokka.velocypack._
import scodec.Decoder

import scala.concurrent.Future

class Collection(val database: Database, val collection: String) {

  def document[T: Decoder](key: String): Future[Either[VPackError, Response[T]]] = database.document(s"$collection/$key")

  def count() = {
    database.session.exec[Unit, api.CollectionCount](Request(
      RequestHeader(
        database = database.database,
        requestType = RequestType.GET,
        request = s"/_api/collection/$collection/count",
      ), ())).value
  }

  def info() = database.collection(collection)

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
   * @return
   */
  def checksum(withRevisions: Boolean = false, withData: Boolean = false) = {
    database.session.exec[Unit, api.CollectionChecksum](Request(
      RequestHeader(
        database = database.database,
        requestType = RequestType.GET,
        request = s"/_api/collection/$collection/checksum",
        parameters = Map("withRevisions" -> withRevisions.toString, "withData" -> withData.toString)
      ), ())).value
  }
}
