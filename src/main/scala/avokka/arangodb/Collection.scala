package avokka.arangodb

import avokka.velocypack._

import scala.concurrent.Future

class Collection(val database: Database, collectionName: String) {

  lazy val name = CollectionName(collectionName)

  def get[C, O](c: C)(implicit command: api.Api.Aux[Collection, C, O], decoder: VPackDecoder[O]): Future[Either[VPackError, Response[O]]] = {
    database.session.exec(command.requestHeader(this, c))(decoder).value
  }

  def apply[C, T, O](c: C)(implicit command: api.ApiPayload.Aux[Collection, C, T, O], encoder: VPackEncoder[T], decoder: VPackDecoder[O]): Future[Either[VPackError, Response[O]]] = {
    database.session.exec(Request(command.requestHeader(this, c), command.body(this, c)))(command.bodyEncoder, decoder).value
  }

  def document[T](key: DocumentKey)(implicit d: VPackDecoder[T]): Future[Either[VPackError, Response[T]]] = database.document(DocumentHandle(name, key))(d)

  def drop(isSystem: Boolean = false) = {
    database.session.exec[api.CollectionDrop](Request.Header(
      database = database.name,
      requestType = RequestType.DELETE,
      request = s"/_api/collection/$name",
      parameters = Map("isSystem" -> isSystem.toString)
    )).value
  }

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

  def count() = {
    database.session.exec[api.CollectionCount](Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name/count",
    )).value
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
    database.session.exec[api.CollectionChecksum](Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = s"/_api/collection/$name/checksum",
      parameters = Map("withRevisions" -> withRevisions.toString, "withData" -> withData.toString)
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
