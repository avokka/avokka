package avokka.arangodb
package api

import avokka.velocypack._

final case class GraphInfo(
    name: String
)

object GraphInfo { self =>

  /** A definition for the relations of the graph
    *
    * @param collection Name of the edge collection, where the edge are stored in.
    * @param from List of vertex collection names. Edges in collection can only be inserted if their _from is in any of the collections here.
    * @param to List of vertex collection names. Edges in collection can only be inserted if their _to is in any of the collections here.
    */
  final case class GraphEdgeDefinition(
      collection: String,
      from: List[String],
      to: List[String]
  )
  object GraphEdgeDefinition {
    implicit val decoder: VPackDecoder[GraphEdgeDefinition] =
      VPackDecoder.gen
  }

  /** The information about a graph
    *
    * @param _id The internal id value of this graph.
    * @param _rev The revision of this graph. Can be used to make sure to not override concurrent modifications to this graph.
    * @param name The name of the graph.
    * @param edgeDefinitions An array of definitions for the relations of the graph. Each has the following type:
    * @param minReplicationFactor The minimal replication factor used for every new collection in the graph. If one shard has less than minReplicationFactor copies, we cannot write to this shard, but to all others.
    * @param numberOfShards Number of shards created for every new collection in the graph.
    * @param orphanCollections An array of additional vertex collections. Documents within these collections do not have edges within this graph.
    * @param replicationFactor The replication factor used for every new collection in the graph.
    * @param isSmart Flag if the graph is a SmartGraph (Enterprise Edition only) or not.
    * @param smartGraphAttribute The name of the sharding attribute in smart graph case (Enterprise Edition only)
    */
  final case class GraphRepresentation(
      _id: String,
      _rev: String,
      name: String,
      edgeDefinitions: List[GraphEdgeDefinition] = List.empty,
      minReplicationFactor: Option[Int] = None,
      numberOfShards: Option[Int] = None,
      orphanCollections: List[String] = List.empty,
      replicationFactor: Option[Int] = None,
      isSmart: Boolean = false,
      smartGraphAttribute: Option[String] = None
  )

  object GraphRepresentation {
    implicit val decoder: VPackDecoder[GraphRepresentation] =
      VPackDecoder.gen
  }

  final case class Response(
      graph: GraphRepresentation
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

/*  implicit val api: Api.EmptyBody.Aux[ArangoDatabase, GraphInfo, Response] =
    new Api.EmptyBody[ArangoDatabase, GraphInfo] {
      override type Response = self.Response
      override def header(database: ArangoDatabase, command: GraphInfo): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.GET,
          request = s"/_api/gharial/${command.name}",
        )
    }*/
}
