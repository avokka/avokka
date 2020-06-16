package avokka

import cats.data.OptionT
import cats.effect.Sync
import cats.effect.concurrent.Ref
import scodec.bits.ByteVector
import cats.syntax.functor._

trait VChunkHistory[F[_]] {
  /** push a chunk in stack and get back full message if complete
    *
    * @param c chunk received
    * @return some message id -> data if complete, else none
    */
  def push(c: VChunk): F[Option[(Long, ByteVector)]]
}

object VChunkHistory {
  def apply[F[_] : Sync]: F[VChunkHistory[F]] = {
    for {
      stack <- Ref.of(Vector.empty[VChunk])
    } yield new VChunkHistory[F] {
      override def push(c: VChunk): F[Option[(Long, ByteVector)]] = if (c.header.x.single) {
        Sync[F].pure(Some(c.header.message -> c.data))
      } else (for {
          // push chunk in history
          st <- OptionT.liftF(stack.updateAndGet(_ :+ c))
          // fetch chunks with same message id
          siblings = st.filter(_.header.message == c.header.message)
          // check if first message index equals number of chunks in history
          first <- OptionT.fromOption(siblings.map(_.header.x).find(_.first)) if first.index == siblings.size
          // clean complete chunks
          _ <- OptionT.liftF(stack.update(_.filterNot(_.header.message == c.header.message)))
          // reconstruct data message
          data = siblings.sortBy(_.header.x.position).map(_.data).reduce(_ ++ _)
        } yield c.header.message -> data).value
    }
  }
}
