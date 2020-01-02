package avokka.arangodb

import akka.NotUsed
import akka.stream.scaladsl.Source
import avokka.arangodb.api.{Api, Cursor}
import avokka.velocypack.{VPackDecoder, VPackEncoder}

class Database(val session: Session, databaseName: String) extends ApiContext[Database] {

  lazy val name: DatabaseName = DatabaseName(databaseName)

  def source[C, T](c: C)(
    implicit api: Api.Command.Aux[Database, C, Cursor.Response[T]],
    ce: VPackEncoder[C],
    td: VPackDecoder[T]): Source[T, NotUsed] =
    Source.fromGraph(
      new CursorSource[C, T](c, this)(api, ce, td)
    )
}

object Database {
  val systemName: DatabaseName = DatabaseName("_system")
}
