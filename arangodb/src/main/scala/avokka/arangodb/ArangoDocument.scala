package avokka.arangodb

import avokka.arangodb.types.DocumentKey

trait ArangoDocument[F[_]] {
  def key: DocumentKey
}
