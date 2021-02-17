package avokka.arangodb

import akka.NotUsed
import akka.stream.scaladsl.Source
import avokka.arangodb.api.Cursor
import avokka.arangodb.types.{CollectionName, DocumentHandle, DocumentKey}
import avokka.velocypack._

class ArangoCollectionF[F[_]](val database: ArangoDatabase[F], val name: CollectionName) {

  def all[T]: Cursor[VObject, T] = Cursor[VObject, T](
    query = "FOR doc IN @@collection RETURN doc",
    bindVars = VObject("@collection" -> name.toVPack)
  )

  def lookup[T](keys: Iterable[DocumentKey]): Cursor[VObject, List[T]] = Cursor[VObject, List[T]](
    query = "RETURN DOCUMENT(@@collection, @keys)",
    bindVars = VObject("@collection" -> name.toVPack, "keys" -> keys.toVPack)
  )

  def upsert[T](key: DocumentKey, obj: VObject): Cursor[VObject, T] = {
    val kvs = obj.values.keys
      .map { key => s"$key:@$key" }
      .mkString(",")
    Cursor[VObject, T](
      s"UPSERT {_key:@_key} INSERT {_key:@_key,$kvs} UPDATE {$kvs} IN @@collection RETURN NEW",
      obj.updated("@collection", name).updated("_key", key)
    )
  }

  /*
  def source[T: VPackDecoder](batchSize: Long): Source[T, NotUsed] =
    database.source(all[T].withBatchSize(batchSize))

   */
}
