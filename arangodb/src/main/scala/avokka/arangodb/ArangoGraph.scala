package avokka.arangodb

import avokka.arangodb.types.DatabaseName
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
}

object ArangoGraph {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, _name: String): ArangoGraph[F] = new ArangoGraph[F] {
    override def name: String = _name

    private val path: String = "/_api/gharial/" + _name

    override def create(edgeDefinitions: List[GraphEdge], orphanCollections: List[String]): F[ArangoResponse[GraphInfo]] =
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
  }
}
