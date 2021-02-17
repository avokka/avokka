package avokka.arangodb

trait ArangoIndex[F[_]] {
  def collection: ArangoCollection[F]
  def name: String

}
