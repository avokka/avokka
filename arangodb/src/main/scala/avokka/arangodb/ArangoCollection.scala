package avokka.arangodb

import avokka.arangodb.types.CollectionName

trait ArangoCollection[F[_]] {
  def name: CollectionName
}

object ArangoCollection {
  def apply[F[_]](_name: CollectionName): ArangoCollection[F] = new ArangoCollection[F] {
    override def name: CollectionName = _name
  }
}