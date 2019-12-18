package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

class Collection(val database: Database, collectionName: String) extends ApiContext[Collection] {

  lazy val name = CollectionName(collectionName)

  lazy val session = database.session

  def document[T](key: DocumentKey)(implicit d: VPackDecoder[T]): Future[Either[VPackError, Response[T]]] = database.document(DocumentHandle(name, key))(d)

  def truncate() = {
    database.session.exec[api.CollectionInfo](Request.Header(
      database = database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/$name/truncate",
    )).value
  }

  def unload() = {
    database.session.exec[api.CollectionInfo](Request.Header(
      database = database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/$name/unload",
    )).value
  }

  def info() = {
    database.session.exec[api.CollectionInfo](Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name",
    )).value
  }

  def properties() = {
    database.session.exec[api.CollectionProperties](Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name/properties",
    )).value
  }

  def revision() = {
    database.session.exec[api.CollectionRevision](Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name/revision",
    )).value
  }
}
