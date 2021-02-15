package avokka.arangodb
package api

import avokka.velocypack._

final case class GraphList()

object GraphList { self =>

  /**
    * @param graphs list of graph representations
    */
  final case class Response(
      graphs: Vector[GraphInfo.GraphRepresentation]
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, GraphList, Response] =
    new Api.EmptyBody[ArangoDatabase, GraphList] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: GraphList): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = "/_api/gharial",
        )
    }
}
