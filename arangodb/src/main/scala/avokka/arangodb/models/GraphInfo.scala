package avokka.arangodb
package models

import types.{CollectionName, GraphName}
import avokka.velocypack._

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
final case class GraphInfo(
  _id: String,
  _rev: String,
  name: GraphName,
  edgeDefinitions: List[GraphEdgeDefinition] = List.empty,
  minReplicationFactor: Option[Int] = None,
  numberOfShards: Option[Int] = None,
  orphanCollections: List[CollectionName] = List.empty,
  replicationFactor: Option[Int] = None,
  isSmart: Boolean = false,
  smartGraphAttribute: Option[String] = None
)

object GraphInfo { self =>
  implicit val decoder: VPackDecoder[GraphInfo] = VPackDecoder.gen

  final case class Response(
      graph: GraphInfo
  )
  object Response {
    implicit val decoder: VPackDecoder[Response] = VPackDecoder.gen
  }

}
