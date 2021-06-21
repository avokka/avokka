package avokka.arangodb
package fs2

import avokka.velocystream._
import cats.data.{Chain, OptionT}
import cats.effect.{Concurrent, Ref}
import cats.syntax.foldable._
import cats.syntax.functor._
import scodec.interop.cats.ByteVectorMonoidInstance

trait VChunkAssembler[F[_]] {

  /** push a chunk and get back full message if complete
    *
    * @param chunk chunk received
    * @return some message if complete, else none
    */
  def push(chunk: VStreamChunk): OptionT[F, VStreamMessage]

  // for testing purposes
  private[avokka] def size: F[Long]
}

object VChunkAssembler {
  def apply[F[_]: Concurrent]: F[VChunkAssembler[F]] =
    for {
      // history of incomplete chunks received
      stack <- Ref.of(Chain.empty[VStreamChunk])
    } yield new VChunkAssembler[F] {
      override def push(c: VStreamChunk): OptionT[F, VStreamMessage] =
        if (c.header.x.single) {
          // message is not split, bypass stack
          OptionT.pure[F](VStreamMessage(c.header.id, c.data))
        } else for {
          // push chunk in history
          st <- OptionT.liftF(stack.updateAndGet(_ :+ c))
          // fetch chunks with same message id
          same = st.filter(_.header.id == c.header.id)
          // check if first message index equals number of chunks collected
          _ <- OptionT.fromOption[F](same.map(_.header.x).find(x => x.first && (x.index == same.size)))
          // remove chunks when message is complete
          _ <- OptionT.liftF(stack.update(_.filterNot(_.header.id == c.header.id)))
          // reconstruct message payload
          data = same.sortBy(_.header.x.position).foldMap(_.data)
        } yield VStreamMessage(c.header.id, data)

      override private[avokka] def size: F[Long] = stack.get.map(_.size)
    }

}
