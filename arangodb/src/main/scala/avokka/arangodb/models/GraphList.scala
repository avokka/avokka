package avokka.arangodb
package models

import avokka.velocypack._

/**
  * @param graphs list of graph representations
  */
final case class GraphList(
                            graphs: Vector[GraphInfo.GraphRepresentation]
                          )

object GraphList {

  implicit val decoder: VPackDecoder[GraphList] = VPackDecoder.gen


/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, GraphList, Response] =
    new Api.EmptyBody[ArangoDatabase, GraphList] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: GraphList): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = "/_api/gharial",
        )
    }*/
}
