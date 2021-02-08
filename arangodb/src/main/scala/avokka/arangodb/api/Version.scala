package avokka.arangodb
package api

import avokka.velocypack._
import types._

final case class Version(
    details: Boolean = false
) {
  def parameters = Map(
    "details" -> details.toString
  )
}

object Version { self =>

  /**
    * @param server  will always contain arango
    * @param license the server license
    * @param version the server version string. The string has the format
    *                "major.minor.sub". major and minor will be numeric, and sub
    *                may contain a number or a textual version
    * @param details additional information about included components and their versions
    */
  final case class Response(
      server: String,
      license: String,
      version: String,
      details: Map[String, String] = Map.empty
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  implicit val api: Api.EmptyBody.Aux[ArangoSession, Version, Response] = new Api.EmptyBody[ArangoSession, Version] {
    override type Response = self.Response
    override def header(session: ArangoSession, command: Version): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = DatabaseName.system,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = command.parameters
    )
  }
}
