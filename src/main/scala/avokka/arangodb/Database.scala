package avokka.arangodb

import avokka.velocypack._

class Database(session: Session, name: String = "_system") {

  def version(details: Boolean = false) = {
    session.exec[Unit, api.Version](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = Map("details" -> details.toString)
    ), ())).value
  }

  def engine() = {
    session.exec[Unit, api.Engine](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    ), ())).value
  }

  def databases() = {
    session.exec[Unit, api.Database](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/database",
    ), ())).value
  }

  def collections(excludeSystem: Boolean = false) = {
    session.exec[Unit, api.Collection](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map("excludeSystem" -> excludeSystem.toString)
    ), ())).value
  }

  def adminEcho() = {
    session.exec[Unit, api.admin.AdminEcho](Request(RequestHeader(
      database = name,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    ), ())).value
  }
}
