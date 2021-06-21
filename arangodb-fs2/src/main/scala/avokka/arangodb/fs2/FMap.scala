package avokka.arangodb.fs2

import cats.effect.{Concurrent, Ref}
import cats.syntax.functor._

import scala.collection.immutable.TreeMap

trait FMap[F[_], K, V] {
  def update(key: K, result: V): F[Unit]
  def remove(key: K): F[Option[V]]
}

object FMap {

  def apply[F[_]: Concurrent, K: Ordering, V]: F[FMap[F, K, V]] =
    for {
      mm <- Ref.of[F, Map[K, V]](TreeMap.empty[K, V])
    } yield new FMap[F, K, V] {
      override def update(key: K, result: V): F[Unit] = mm.update(m => m.updated(key, result))
      override def remove(key: K): F[Option[V]] = mm.modify(m => (m - key, m.get(key)))
    }

}

