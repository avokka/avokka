package avokka.arangodb

import avokka.velocypack._
import scodec.{Codec, Decoder, Encoder}

class Database(val session: Session, val database: String = "_system") {

  def version(details: Boolean = false) = {
    session.exec[Unit, api.Version](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = Map("details" -> details.toString)
    ), ())).value
  }

  def engine() = {
    session.exec[Unit, api.Engine](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = "/_api/engine"
    ), ())).value
  }

  def collections(excludeSystem: Boolean = false) = {
    session.exec[Unit, api.Collections](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map("excludeSystem" -> excludeSystem.toString)
    ), ())).value
  }

  def collection(collection: String) = {
    session.exec[Unit, api.Collection](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = s"/_api/collection/$collection",
    ), ())).value
  }

  def document[T: Decoder](handle: String) = {
    session.exec[Unit, T](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = s"/_api/document/$handle"
    ), ())).value
  }

  def cursor[V: Codec, T: Codec](cursor: api.Cursor[V]) = {
    session.exec[api.Cursor[V], api.Cursor.Response[T]](Request(RequestHeader(
      database = database,
      requestType = RequestType.POST,
      request = s"/_api/cursor"
    ), cursor)).value
  }

}
