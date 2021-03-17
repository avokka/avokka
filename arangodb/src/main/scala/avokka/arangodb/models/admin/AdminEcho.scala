package avokka.arangodb
package models
package admin

import avokka.velocypack._

object AdminEcho { self =>

  final case class Server(
      address: String,
      port: Long,
      endpoint: String,
  )

  object Server {
    implicit val decoder: VPackDecoder[Server] = VPackDecoder.gen
  }

  final case class Client(
      address: String,
      port: Long,
      id: String,
  )
  object Client {
    implicit val decoder: VPackDecoder[Client] = VPackDecoder.gen
  }

  /**
    * @param authorized whether the session is authorized
    * @param client
    * @param cookies list of the cookies you sent
    * @param database the database this request was executed on
    * @param headers the list of the HTTP headers you sent
    * @param internals contents of the server internals struct
    * @param parameters Object containing the query parameters
    * @param `path` relative path of this request
    * @param prefix prefix of the database
    * @param protocol the transport, one of ['http', 'https', 'velocystream']
    * @param rawRequestBody List of digits of the sent characters
    * @param rawSuffix
    * @param requestBody stringified version of the POST body we sent
    * @param requestType In this case *POST*, if you use another HTTP-Verb, you will se that (GET/DELETE, ...)
    * @param server
    * @param suffix
    * @param url the raw request URL
    * @param user the currently user that sent this request
    */
  final case class Response(
      authorized: Boolean,
      client: Client,
      //  cookies: Map[String, String],
      database: String,
      headers: Map[String, String],
      internals: Map[String, String],
      parameters: Map[String, String],
      `path`: String,
      portType: String,
      prefix: String,
      protocol: String,
      // rawRequestBody: Option[List[Any]],
      // rawSuffix: Option[List[Any]],
      requestBody: String,
      requestType: String,
      server: Server,
      // suffix: Option[List[Any]],
      url: String,
      user: String,
  )

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoSession, AdminEcho.type, Response] = new Api.EmptyBody[ArangoSession, AdminEcho.type] {
    override type Response = self.Response
    override def header(session: ArangoSession, command: AdminEcho.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = DatabaseName.system,
      requestType = RequestType.POST,
      request = "/_admin/echo"
    )
  }*/
}
