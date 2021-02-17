package avokka.arangodb

import avokka.arangodb.types.DatabaseName

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

}

object ArangoDatabase {
  def apply[F[_]](_name: DatabaseName): ArangoDatabase[F] = new ArangoDatabase[F] {
    override def name: DatabaseName = _name
  }
}