package avokka.arangodb.api

import avokka.arangodb.{Database, Request, RequestType}
import avokka.velocypack._

object Engine { self =>

  /**
   * the storage engine the server is configured to use
   *
   * @param name will be mmfiles or rocksdb
   * @param supports what the engine supports
   */
  case class Response
  (
    name: String,
    supports: Supports
  )

  case class Supports
  (
    dfdb: Boolean,
    indexes: List[String],
  )
  object Supports {
    implicit val decoder: VPackDecoder[Supports] = VPackRecord[Supports].decoder
  }

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.Aux[Database, Engine.type, Response] = new Api[Database, Engine.type] {
    override def requestHeader(database: Database, command: Engine.type): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    )
    override type Response = self.Response
  //  override def responseDecoder: VPackDecoder[Response] = Response.decoder
  }
}