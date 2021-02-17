package avokka.arangodb

import avokka.arangodb.types.DatabaseName

class ArangoDatabaseF(val session: ArangoSession, val name: DatabaseName) {

  /*
  def source[C, T](c: C)(
    implicit api: Api.Command.Aux[ArangoDatabase, C, Cursor.Response[T]],
    decoder: VPackDecoder[T]): Source[T, NotUsed] =
    Source.fromGraph(
      new CursorSource(c, this)(api, decoder)
    )

   */
}
