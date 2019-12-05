package avokka.arangodb

import avokka.{RequestType, VRequest, VRequestHeader, VResponse, VSession}
import avokka.velocypack._
import avokka.velocystream._
import cats.data.Validated

import scala.concurrent.Future

class AvokkaDatabase(session: VSession, name: String = "_system") {

  def apiVersion(details: Boolean = false): Future[Validated[VPackError, VResponse[ApiVersion]]] = {
    session.exec[Unit, ApiVersion](VRequest(VRequestHeader(database = name, requestType = RequestType.GET, request = "/_api/version"), ()))
  }

  def collections() = {
    session.exec[Unit, ApiCollection](VRequest(VRequestHeader(database = name, requestType = RequestType.GET, request = "/_api/collection"), ()))
  }

}
