package avokka.arangodb.api.admin

import avokka.velocypack._
import avokka.velocypack.codecs.VPackRecordCodec
import scodec.Codec
import scodec.bits.ByteVector

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
  client: VPackArray, //Object, // AdminEcho.Client,
//  cookies: Map[String, String],
  database: String,
  headers: Map[String, String],
  internals: Map[String, String],
  parameters: Map[String, String],
  `path`: String,
  prefix: Map[String, String],
  protocol: String,
  // rawRequestBody: Option[List[Any]],
  // rawSuffix: Option[List[Any]],
  requestBody: String,
  requestType: String,
  server: VPackObject, // AdminEcho.Server,
  // suffix: Option[List[Any]],
  url: String,
  user: String,
)

object AdminEcho {
  case class Server
  (
    address: Long,
    port: Long,
    endpoint: String,
  )

  case class Client
  (
  //  address: String,
    port: Long,
    id: String,
  )

  implicit val codec: Codec[AdminEcho] = VPackRecordCodec[AdminEcho].codec

}