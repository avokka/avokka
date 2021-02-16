package avokka.arangodb

import avokka.arangodb.types.DatabaseName

trait ArangoClient[F[_]] {
  def database(name: DatabaseName): ArangoDatabase[F]

  def version(details: Boolean = false): F[api.Version.Response]
}
