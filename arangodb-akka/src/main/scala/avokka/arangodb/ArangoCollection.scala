package avokka.arangodb

import avokka.arangodb.api.Query
import avokka.arangodb.types.{CollectionName, DocumentKey}
import avokka.velocypack._

class ArangoCollectionF[F[_]](val database: ArangoDatabase[F], val name: CollectionName) {

  /*
  def source[T: VPackDecoder](batchSize: Long): Source[T, NotUsed] =
    database.source(all[T].withBatchSize(batchSize))

   */
}
