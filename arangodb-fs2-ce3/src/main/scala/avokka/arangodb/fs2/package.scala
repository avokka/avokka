package avokka.arangodb

import avokka.velocypack.VPackDecoder

import _root_.fs2._

package object fs2 {
  implicit def arangoFs2Stream[F[_]]: ArangoStream.Aux[F, Stream] = new ArangoStream[F] {
    type S[A[_], B] = Stream[A, B]

    override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[F, V]): S[F, T] =
      for {
        cursor <- Stream.eval(query.cursor[T])
        document <- Pull
          .loop { (c: ArangoCursor[F, T]) =>
            Pull.output(Chunk.vector(c.body.result)) >>
              (if (c.body.hasMore) Pull.eval(c.next()).map(Some(_)) else Pull.pure(None))
          }(cursor)
          .void
          .stream
      } yield document

  }
}
