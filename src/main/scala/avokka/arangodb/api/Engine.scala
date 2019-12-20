package avokka.arangodb
package api

import avokka.velocypack._

object Engine { self =>

  /**
    * the storage engine the server is configured to use
    *
    * @param name will be mmfiles or rocksdb
    * @param supports what the engine supports
    */
  case class Response(
      name: String,
      supports: Supports
  )

  case class Supports(
      dfdb: Boolean,
      indexes: List[String],
  )
  object Supports {
    implicit val decoder: VPackDecoder[Supports] = VPackRecord[Supports].decoder
  }

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackRecord[Response].decoder
  }

  implicit val api: Api.EmptyBody.Aux[Database, Engine.type, Response] = new Api.EmptyBody[Database, Engine.type] {
    override type Response = self.Response
    override def header(database: Database, command: Engine.type): Request.HeaderTrait = Request.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    )
  }
}
