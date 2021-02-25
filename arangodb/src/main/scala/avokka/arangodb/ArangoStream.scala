package avokka.arangodb

import avokka.velocypack._

trait ArangoStream[F[_]] {
  type S[_[_], _]
  def fromQuery[V, T: VPackDecoder](query: ArangoQuery[F, V]): S[F, T]
//  def evalMap[T, U](s: S[F, T])(f: T => F[U]): S[F, U]
}

object ArangoStream {
  type Aux[F[_], S_[_[_], _]] = ArangoStream[F] { type S[F_[_], T] = S_[F_, T] }
}
