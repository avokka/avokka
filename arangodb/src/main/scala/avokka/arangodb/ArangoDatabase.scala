package avokka.arangodb

import avokka.arangodb.types.DatabaseName

trait ArangoDatabase[F[_]] {
  def name: DatabaseName

}
