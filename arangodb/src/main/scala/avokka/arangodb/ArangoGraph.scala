package avokka.arangodb

import avokka.arangodb.types.{CollectionName, DatabaseName}
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import models._
import protocol._

trait ArangoGraph[F[_]] {
  /** graph name */
  def name: String

  def create(
    edgeDefinitions: List[GraphEdge] = List.empty,
    orphanCollections: List[String] = List.empty,
    waitForSync: Boolean = false,
  ): F[ArangoResponse[GraphInfo]]

  /** Get graph information */
  def info(): F[ArangoResponse[GraphInfo]]

  /**
    * Drop the graph
    *
    * @param dropCollections Drop collections of this graph as well. Collections will only be dropped if they are not used in other graphs.
    * @return deletion result
    */
  def drop(dropCollections: Boolean = false): F[ArangoResponse[Boolean]]

  def vertexes(): F[ArangoResponse[Vector[CollectionName]]]

}

object ArangoGraph {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, _name: String): ArangoGraph[F] = new ArangoGraph[F] {
    override def name: String = _name

    private val path: String = "/_api/gharial/" + _name

    override def create(
      edgeDefinitions: List[GraphEdge],
      orphanCollections: List[String],
      waitForSync: Boolean,
    ): F[ArangoResponse[GraphInfo]] =
      POST(
        database,
        "/_api/gharial/",
        Map(
          "waitForSync" -> waitForSync.toString,
        )
      ).body(
        VObject(
          "name" -> name.toVPack,
          "edgeDefinitions" -> edgeDefinitions.toVPack,
          "orphanCollections" -> orphanCollections.toVPack
        )
      )
        .execute[F, GraphInfo.Response]
        .map(_.map(_.graph))

    override def info(): F[ArangoResponse[GraphInfo]] =
      GET(database, path)
        .execute[F, GraphInfo.Response]
        .map(_.map(_.graph))

    override def drop(dropCollections: Boolean): F[ArangoResponse[Boolean]] =
      DELETE(database, path, Map("dropCollections" -> dropCollections.toString))
        .execute[F, RemovedResult]
        .map(_.map(_.removed))

    override def vertexes(): F[ArangoResponse[Vector[CollectionName]]] =
      GET(database, path + "/vertex")
        .execute[F, GraphCollections]
        .map(_.map(_.collections))
  }
}
