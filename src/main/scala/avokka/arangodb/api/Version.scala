package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack._

case class Version
(
  details: Boolean = false
)
{
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
  case class Response
  (
    server: String,
    license: String,
    version: String,
    details: Map[String, String] = Map.empty
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoderWithDefaults
  }

  implicit val api: Api.Aux[Database, Version, Response] = new Api[Database, Version] {
    override type Response = self.Response
    override def requestHeader(database: Database, command: Version): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/version",
      parameters = command.parameters
    )
   // override val responseDecoder: VPackDecoder[Response] = Response.decoder
  }
}
