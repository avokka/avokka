package avokka.arangodb

import avokka.arangodb.api.DocumentRead
import avokka.velocypack.VPackDecoder

class Collection(val database: Database, collectionName: String) extends ApiContext[Collection] {

  lazy val name: CollectionName = CollectionName(collectionName)

  override lazy val session: Session = database.session

  // def document[T](key: DocumentKey)(implicit d: VPackDecoder[T]): Future[Either[VPackError, Response[T]]] = database.document(DocumentHandle(name, key))(d)
  def handle(key: DocumentKey): DocumentHandle = DocumentHandle(name, key)

  def read[T: VPackDecoder](key: DocumentKey) = database(DocumentRead[T](handle(key)))
}
