package avokka.arangodb

import _root_.akka.NotUsed
import _root_.akka.stream.scaladsl.Source
import avokka.velocypack.VPackDecoder

import scala.concurrent.Future

package object akka {
  type AkkaStream[_[_], T] = Source[T, NotUsed]

  implicit val arangoStreamAkkaStream: ArangoStream.Aux[Future, AkkaStream] =
    new ArangoStream[Future] {
      type S[F[_], T] = AkkaStream[F, T]

      override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[Future, V]): Source[T, NotUsed] = Source.fromGraph(
        new ArangoCursorSource(query)
      )
    }
}
