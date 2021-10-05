package avokka.arangodb

import avokka.arangodb.types.DatabaseName
import avokka.velocypack._
import models._
import protocol._

trait ArangoGraph[F[_]] {
  /** graph name */
  def name: String

  def create(
              edgeDefinitions: List[GraphInfo.GraphEdgeDefinition] = List.empty,
              orphanCollections: List[String] = List.empty,
            ): F[ArangoResponse[GraphInfo.Response]]

  /** Get graph information */
  def info(): F[ArangoResponse[GraphInfo.Response]]

  /**
    * Drop the graph
    *
    * @param dropCollections Drop collections of this graph as well. Collections will only be dropped if they are not used in other graphs.
    * @return deletion result
    */
  def drop(dropCollections: Boolean = false): F[ArangoResponse[GraphInfo.DeleteResult]]
}

object ArangoGraph {
  def apply[F[_] : ArangoClient](database: DatabaseName, _name: String): ArangoGraph[F] = new ArangoGraph[F] {
    override def name: String = _name

    private val path: String = "/_api/gharial/" + _name

    override def create(edgeDefinitions: List[GraphInfo.GraphEdgeDefinition], orphanCollections: List[String]): F[ArangoResponse[GraphInfo.Response]] =
      POST(
        database,
        "/_api/gharial/"
      ).body(
        VObject(
          "name" -> name.toVPack,
          "edgeDefinitions" -> edgeDefinitions.toVPack,
          "orphanCollections" -> orphanCollections.toVPack
        )
      )
        .execute

    override def info(): F[ArangoResponse[GraphInfo.Response]] =
      GET(database, path).execute

    override def drop(dropCollections: Boolean): F[ArangoResponse[GraphInfo.DeleteResult]] =
      DELETE(database, path, Map("dropCollections" -> dropCollections.toString)).execute
  }
}
