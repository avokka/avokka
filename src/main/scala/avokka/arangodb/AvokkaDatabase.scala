package avokka.arangodb

import avokka.velocypack._
import avokka.velocystream._
import cats.data.Validated

import scala.concurrent.Future

class AvokkaDatabase(session: VSession, name: String = "_system") {

  def apiVersion(): Future[Validated[VPackError, VResponse[ApiVersion]]] = {
    session.exec[Unit, ApiVersion](VRequest(VRequestHeader(1, 1, name, 1, "/_api/version"), ()))
  }

  def collections() = {
    session.exec[Unit, VPackObject](VRequest(VRequestHeader(1, 1, name, 1, "/_api/collection"), ()))
  }

}
