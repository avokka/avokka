package avokka.arangodb
package api

import avokka.velocypack._

case class Version(
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
  case class Response(
      server: String,
      license: String,
      version: String,
      details: Map[String, String] = Map.empty
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.EmptyBody.Aux[Session, Version, Response] = new Api.EmptyBody[Session, Version] {
    override type Response = self.Response
    override def header(session: Session, command: Version): Request.HeaderTrait = Request.Header(
      database = DatabaseName.system,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = command.parameters
    )
  }
}
