package avokka.arangodb

import avokka.arangodb.types.DatabaseName
import models._
import protocol._

trait ArangoGraph[F[_]] {
  /** graph name */
  def name: String

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

    override def info(): F[ArangoResponse[GraphInfo.Response]] =
      GET(database, path).execute

    override def drop(dropCollections: Boolean): F[ArangoResponse[GraphInfo.DeleteResult]] =
      DELETE(database, path, Map("dropCollections" -> dropCollections.toString)).execute
  }
}
