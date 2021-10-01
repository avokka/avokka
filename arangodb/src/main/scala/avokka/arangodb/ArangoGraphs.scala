package avokka.arangodb

import models._
import avokka.velocypack._
import cats.Functor
import cats.syntax.functor._
import types._
import protocol._

/**
  * ArangoDB graphs API (gharial)
  *
  * @see [[https://www.arangodb.com/docs/stable/http/gharial.html]]
  * @tparam F effect
  */
trait ArangoGraphs[F[_]] {

  /**
    * List all graphs
    *
    * @return all graphs known to the graph module
    */
  def list(): F[ArangoResponse[GraphList]]

}

object ArangoGraphs {

  def apply[F[_]: ArangoClient: Functor](database: DatabaseName): ArangoGraphs[F] = new ArangoGraphs[F] {

    override def list(): F[ArangoResponse[GraphList]] =
      GET(database, "/_api/gharial").execute

  }
}
