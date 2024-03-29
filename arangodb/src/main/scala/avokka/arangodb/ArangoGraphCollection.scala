package avokka.arangodb

import types._
import cats.Functor
import protocol._

trait ArangoGraphCollection[F[_]] {
  /** The name of the vertices collection. */
  def name: CollectionName

  def vertex(key: DocumentKey): ArangoGraphVertex[F]

  def edge(key: DocumentKey): ArangoGraphEdge[F]

}

object ArangoGraphCollection {
  def apply[F[_] : ArangoClient : Functor](database: DatabaseName, graph: GraphName, _name: CollectionName): ArangoGraphCollection[F] = new ArangoGraphCollection[F] {

    override def name: CollectionName = _name

    override def vertex(key: DocumentKey): ArangoGraphVertex[F] = ArangoGraphVertex(database, graph, DocumentHandle(name, key))

    override def edge(key: DocumentKey): ArangoGraphEdge[F] = ArangoGraphEdge(database, graph, DocumentHandle(name, key))

  }
}