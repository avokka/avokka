package avokka.arangodb

import akka.NotUsed
import akka.stream.scaladsl.Source
import avokka.velocypack.VPackDecoder

import scala.concurrent.{ExecutionContext, Future}

object akkaStream {
  type AkkaStream[_[_], T] = Source[T, NotUsed]

  implicit def arangoStreamAkkaStream(implicit ec: ExecutionContext): ArangoStream[AkkaStream, Future] =
    new ArangoStream[AkkaStream, Future] {
      override def fromQuery[V, T: VPackDecoder](query: ArangoQuery[Future, V]): Source[T, NotUsed] = Source.fromGraph(
        new CursorSource(query)
      )
    }
}
