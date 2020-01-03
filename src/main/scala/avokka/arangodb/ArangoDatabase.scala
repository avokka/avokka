package avokka.arangodb

import akka.NotUsed
import akka.stream.scaladsl.Source
import avokka.arangodb.api.{Api, Cursor}
import avokka.velocypack.{VPackDecoder, VPackEncoder}

class ArangoDatabase(val session: ArangoSession, val name: DatabaseName) extends ApiContext[ArangoDatabase] {

  def source[C, T](c: C)(
    implicit api: Api.Command.Aux[ArangoDatabase, C, Cursor.Response[T]],
    ce: VPackEncoder[C],
    td: VPackDecoder[T]): Source[T, NotUsed] =
    Source.fromGraph(
      new CursorSource[C, T](c, this)(api, ce, td)
    )
}
