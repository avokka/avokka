package avokka.arangodb

import avokka.velocypack.VPackDecoder
import cats.Applicative
import cats.syntax.all._
import fs2.{Chunk, Stream}

object fs2Stream {
  implicit def arangoFs2Stream[F[_]](implicit F: Applicative[F]): ArangoStream.Aux[F, Stream] = new ArangoStream[F] {
    type S[A[_], B] = Stream[A, B]

    override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[F, V]): S[F, T] =

      Stream.eval(query.cursor[T])
        .flatMap { c =>
          Stream.unfoldLoopEval(c) { c =>
            if (c.body.hasMore) c.next().map { n => (c.body.result, Option(n)) }
            else F.pure((c.body.result, none[ArangoCursor[F, T]]))
          }
        }
        .flatMap(r => Stream.chunk(Chunk.vector(r)))
    /*
      .repeatPull(_.uncons1.flatMap {
        case Some((hd, tl)) if hd.body.hasMore => Pull.output(Chunk.vector(hd.body.result)).as(Some(Stream.eval(hd.next()) ++ tl))
        case Some((hd, tl)) => Pull.output(Chunk.vector(hd.body.result)).as(None)
        case None => Pull.pure(None)
      })
*/
  }
}
