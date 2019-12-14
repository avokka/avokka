package avokka.arangodb

import avokka.arangodb
import avokka.velocypack._

class Database(val session: Session, databaseName: String = "_system") {

  lazy val name = DatabaseName(databaseName)

  def engine() = {
    session.exec[Request.Head, api.Engine](Request.Head(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    ))).value
  }

  def info() = {
    session.exec[Request.Head, api.DatabaseCurrent](Request.Head(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/database/current"
    ))).value
  }

  def collectionCreate(t: api.CollectionCreate, waitForSyncReplication: Int = 1, enforceReplicationFactor: Int = 1) = {
    session.exec[Request.Payload[api.CollectionCreate], api.CollectionInfo](Request.Payload(RequestHeader(
      database = name,
      requestType = RequestType.POST,
      request = s"/_api/collection",
      parameters = Map(
        "waitForSyncReplication" -> waitForSyncReplication.toString,
        "enforceReplicationFactor" -> enforceReplicationFactor.toString
      )
    ), t)).value
  }

  def collections(excludeSystem: Boolean = false) = {
    session.exec[Request.Head, api.CollectionList](Request.Head(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map(
        "excludeSystem" -> excludeSystem.toString
      )
    ))).value
  }

  def document[T](handle: DocumentHandle)(implicit d: VPackDecoder[T]) = {
    session.exec[Request.Head, T](Request.Head(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = s"/_api/document/${handle.path}"
    ))).value
  }

  def cursor[V: VPackEncoder, T: VPackDecoder](cursor: api.Cursor[V]) = {
    session.exec[Request.Payload[api.Cursor[V]], api.Cursor.Response[T]](Request.Payload(RequestHeader(
      database = name,
      requestType = RequestType.POST,
      request = s"/_api/cursor"
    ), cursor)).value
  }

}
