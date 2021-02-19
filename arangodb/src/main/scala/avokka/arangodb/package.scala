package avokka

import avokka.velocypack.VPackDecoder

package object arangodb {

  private[avokka] implicit final class AvokkaStringMapUtilsOps(
      private val map: Map[String, Option[String]]
  ) extends AnyVal {
    def collectDefined: Map[String, String] = map.collect {
      case (key, Some(value)) => key -> value
    }
  }

  implicit final class AvokkaQueryStreamOps[S[_[_], _], F[_], V](
      private val query: ArangoQuery[F, V]
  )(implicit S: ArangoStream[S, F]) {
    def stream[T: VPackDecoder]: S[F, T] = S.fromQuery(query)
  }

}
