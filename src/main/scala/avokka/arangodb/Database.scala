package avokka.arangodb

import avokka.velocypack._
import scodec.{Codec, Decoder, Encoder}

class Database(val session: Session, databaseName: String = "_system") {

  lazy val name = DatabaseName(databaseName)

  def engine() = {
    session.exec[Unit, api.Engine](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    ), ())).value
  }

  def info() = {
    session.exec[Unit, api.DatabaseCurrent](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/database/current"
    ), ())).value
  }

  def collectionCreate(t: api.CollectionCreate, waitForSyncReplication: Int = 1, enforceReplicationFactor: Int = 1) = {
    session.exec[api.CollectionCreate, api.CollectionInfo](Request(RequestHeader(
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
    session.exec[Unit, api.CollectionList](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map(
        "excludeSystem" -> excludeSystem.toString
      )
    ), ())).value
  }

  def document[T: Decoder](handle: DocumentHandle) = {
    session.exec[Unit, T](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = s"/_api/document/${handle.path}"
    ), ())).value
  }

  def cursor[V: Codec, T: Codec](cursor: api.Cursor[V]) = {
    session.exec[api.Cursor[V], api.Cursor.Response[T]](Request(RequestHeader(
      database = name,
      requestType = RequestType.POST,
      request = s"/_api/cursor"
    ), cursor)).value
  }

}
