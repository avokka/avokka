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
  final case class Response(
      name: String,
      supports: Supports
  )

  final case class Supports(
      dfdb: Boolean,
      indexes: List[String],
  )
  object Supports {
    implicit val decoder: VPackDecoder[Supports] = VPackDecoder.gen
  }

  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, Engine.type, Response] = new Api.EmptyBody[ArangoDatabase, Engine.type] {
    override type Response = self.Response
    override def header(database: ArangoDatabase, command: Engine.type): ArangoRequest.HeaderTrait = ArangoRequest.Header(
      database = database.name,
      requestType = RequestType.GET,
      request = "/_api/engine"
    )
  }*/
}
