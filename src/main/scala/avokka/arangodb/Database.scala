package avokka.arangodb

import avokka.velocypack._
import scodec.Decoder

class Database(session: Session, val database: String = "_system") {

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
    session.exec[Unit, api.Collection](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map("excludeSystem" -> excludeSystem.toString)
    ), ())).value
  }

  def document[T: Decoder](handle: String) = {
    session.exec[Unit, T](Request(RequestHeader(
      database = database,
      requestType = RequestType.GET,
      request = s"/_api/document/$handle"
    ), ())).value
  }

  def adminEcho() = {
    session.exec[Unit, api.admin.AdminEcho](Request(RequestHeader(
      database = database,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    ), ())).value
  }
}
