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
}
