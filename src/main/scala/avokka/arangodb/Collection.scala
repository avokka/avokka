package avokka.arangodb
import avokka.velocypack.VPackError
import scodec.Decoder

import scala.concurrent.Future

class Collection(session: Session, database: String, collection: String) extends Database(session, database) {
  override def document[T: Decoder](key: String): Future[Either[VPackError, Response[T]]] = super.document(s"$collection/$key")
}
