package avokka

import cats.data.{Chain, OptionT}
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.instances.vector._
import cats.syntax.foldable._
import cats.syntax.functor._
import scodec.bits.ByteVector
import scodec.interop.cats.ByteVectorMonoidInstance

trait VChunkHistory[F[_]] {

  /** push a chunk in stack and get back full message if complete
    *
    * @param c chunk received
    * @return some message id -> data if complete, else none
    */
  def push(c: VChunk): OptionT[F, (Long, ByteVector)]
}

object VChunkHistory {
  def apply[F[_]](implicit F: Sync[F]): F[VChunkHistory[F]] =
    for {
      stack <- Ref.of(Chain.empty[VChunk])
    } yield new VChunkHistory[F] {
      override def push(c: VChunk): OptionT[F, (Long, ByteVector)] =
        if (c.header.x.single) {
          OptionT.pure[F](c.header.message -> c.data)
        } else for {
          // push chunk in history
          st <- OptionT.liftF(stack.updateAndGet(_ :+ c))
          // fetch chunks with same message id
          same = st.filter(_.header.message == c.header.message)
          // check if first message index equals number of chunks collected
          _ <- OptionT.fromOption[F](same.map(_.header.x).find(x => x.first && (x.index == same.size)))
          // clean complete chunks
          _ <- OptionT.liftF(stack.update(_.filterNot(_.header.message == c.header.message)))
          // reconstruct data message
          data = same.toVector.sortBy(_.header.x.position).foldMap(_.data)
        } yield c.header.message -> data
      }

}
