package avokka.arangodb

import avokka.velocypack._

trait ArangoStream[S[_[_], _], F[_]] {
  def fromQuery[V, T: VPackDecoder](query: ArangoQuery[F, V]): S[F, T]
//  def evalMap[T, U](s: S[F, T])(f: T => F[U]): S[F, U]
}

object ArangoStream {

}
