package avokka.arangodb
import avokka.velocypack.VPackError
import scodec.Decoder

import scala.concurrent.Future

class Collection(database: Database, collection: String) {
  def document[T: Decoder](key: String): Future[Either[VPackError, Response[T]]] = database.document(s"$collection/$key")
}
