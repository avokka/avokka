package avokka.arangodb.api.admin

import avokka.velocypack._

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
case class AdminEcho
(
  authorized: Boolean,
  client: AdminEcho.Client,
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
  server: AdminEcho.Server,
  // suffix: Option[List[Any]],
  url: String,
  user: String,
)

object AdminEcho {
  case class Server
  (
    address: String,
    port: Long,
    endpoint: String,
  )

  object Server {
    implicit val decoder: VPackDecoder[Server] = VPackRecord[Server].decoderWithDefaults
  }

  case class Client
  (
    address: String,
    port: Long,
    id: String,
  )
  object Client {
    implicit val decoder: VPackDecoder[Client] = VPackRecord[Client].decoderWithDefaults
  }

  implicit val decoder: VPackDecoder[AdminEcho] = VPackRecord[AdminEcho].decoderWithDefaults

}