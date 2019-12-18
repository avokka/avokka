package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

class Collection(val database: Database, collectionName: String) extends ApiContext[Collection] {

  lazy val name = CollectionName(collectionName)

  lazy val session = database.session

  def document[T](key: DocumentKey)(implicit d: VPackDecoder[T]): Future[Either[VPackError, Response[T]]] = database.document(DocumentHandle(name, key))(d)

}
