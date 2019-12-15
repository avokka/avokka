package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

class Collection(val database: Database, collectionName: String) {

  lazy val name = CollectionName(collectionName)

  def document[T](key: DocumentKey)(implicit d: VPackDecoder[T]): Future[Either[VPackError, Response[T]]] = database.document(DocumentHandle(name, key))(d)

  def documentCreate[T](t: T, returnNew: Boolean = false)(implicit encoder: VPackEncoder[T], decoder: VPackDecoder[T]) = {
    database.session.exec[Request[T], api.DocumentCreate[T]](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.POST,
      request = s"/_api/document/$name"
    ), t)).value
  }

  def drop(isSystem: Boolean = false) = {
    database.session.exec[Request[Unit], api.CollectionDrop](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.DELETE,
      request = s"/_api/collection/$name",
      parameters = Map("isSystem" -> isSystem.toString)
    ), ())).value
  }

  def truncate() = {
    database.session.exec[Request[Unit], api.CollectionInfo](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/$name/truncate",
    ), ())).value
  }

  def unload() = {
    database.session.exec[Request[Unit], api.CollectionInfo](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.PUT,
      request = s"/_api/collection/$name/unload",
    ), ())).value
  }

  def info() = {
    database.session.exec[Request[Unit], api.CollectionInfo](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name",
    ), ())).value
  }

  def properties() = {
    database.session.exec[Request[Unit], api.CollectionProperties](Request(RequestHeader.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name/properties",
    ), ())).value
  }

  def count() = {
    database.session.exec[Request[Unit], api.CollectionCount](Request(
      RequestHeader.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/$name/count",
      ), ())).value
  }

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
    database.session.exec[Request[Unit], api.CollectionChecksum](Request(
      RequestHeader.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/$name/checksum",
        parameters = Map("withRevisions" -> withRevisions.toString, "withData" -> withData.toString)
      ), ())).value
  }

  def revision() = {
    database.session.exec[Request[Unit], api.CollectionRevision](Request(
      RequestHeader.Header(
        database = database.name,
        requestType = RequestType.GET,
        request = s"/_api/collection/$name/revision",
      ), ())).value
  }
}
