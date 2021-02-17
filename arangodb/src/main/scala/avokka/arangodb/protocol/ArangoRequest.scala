package avokka.arangodb
package protocol

import avokka.arangodb.types.DatabaseName
import avokka.velocypack._

final case class ArangoRequest[T](
    header: ArangoRequest.Header,
    body: T
)

object ArangoRequest {

  final case class Header(
      version: Int = 1,
      `type`: MessageType = MessageType.Request,
      database: DatabaseName,
      requestType: RequestType,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty,
  ) {
    def body[T](value: T): ArangoRequest[T] = ArangoRequest(this, value)
  }

  def GET(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header(
    database = database,
    requestType = RequestType.GET,
    request = request,
    parameters = parameters,
    meta = meta
  )

  def DELETE(
      database: DatabaseName,
      request: String,
      parameters: Map[String, String] = Map.empty,
      meta: Map[String, String] = Map.empty
  ): Header = Header(
    database = database,
    requestType = RequestType.DELETE,
    request = request,
    parameters = parameters,
    meta = meta
  )

  def POST(
              database: DatabaseName,
              request: String,
              parameters: Map[String, String] = Map.empty,
              meta: Map[String, String] = Map.empty
            ): Header = Header(
    database = database,
    requestType = RequestType.POST,
    request = request,
    parameters = parameters,
    meta = meta
  )

  def PUT(
            database: DatabaseName,
            request: String,
            parameters: Map[String, String] = Map.empty,
            meta: Map[String, String] = Map.empty
          ): Header = Header(
    database = database,
    requestType = RequestType.PUT,
    request = request,
    parameters = parameters,
    meta = meta
  )

  def PATCH(
           database: DatabaseName,
           request: String,
           parameters: Map[String, String] = Map.empty,
           meta: Map[String, String] = Map.empty
         ): Header = Header(
    database = database,
    requestType = RequestType.PATCH,
    request = request,
    parameters = parameters,
    meta = meta
  )

  object Header {
    implicit val encoder: VPackEncoder[Header] = VPackGeneric[Header].encoder
  }

  final case class Authentication(
      version: Int = 1,
      `type`: MessageType = MessageType.Authentication,
      encryption: String = "plain",
      user: String,
      password: String
  )

  object Authentication {
    implicit val encoder: VPackEncoder[Authentication] =
      VPackGeneric[Authentication].encoder
  }

}
