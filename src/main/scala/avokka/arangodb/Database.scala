package avokka.arangodb

import avokka.arangodb.api.{Collection, Engine, Version}
import avokka.velocypack._
import cats.data.Validated

import scala.concurrent.Future

class Database(session: Session, name: String = "_system") {

  def version(details: Boolean = false): Future[Validated[VPackError, Response[Version]]] = {
    session.exec[Unit, Version](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = Map("details" -> details.toString)
    ), ()))
  }

  def engine() = {
    session.exec[Unit, Engine](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    ), ()))
  }

  def collections(excludeSystem: Boolean = false) = {
    session.exec[Unit, Collection](Request(RequestHeader(
      database = name,
      requestType = RequestType.GET,
      request = "/_api/collection",
      parameters = Map("excludeSystem" -> excludeSystem.toString)
    ), ()))
  }

}
