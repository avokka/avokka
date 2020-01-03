package avokka.arangodb

import avokka.arangodb.api.{Cursor, DocumentRead}
import avokka.velocypack.VPack.VObject
import avokka.velocypack._

class ArangoCollection(val database: ArangoDatabase, val name: CollectionName) extends ApiContext[ArangoCollection] {

  override lazy val session: ArangoSession = database.session

  def handle(key: DocumentKey): DocumentHandle = DocumentHandle(name, key)

  def read[T](key: DocumentKey): DocumentRead[T] = DocumentRead[T](handle(key))

  def all[T]: Cursor[VObject, T] = Cursor[VObject, T](
    query = "FOR doc IN @@collection RETURN doc",
    bindVars = VObject("@collection" -> name.toVPack)
  )

  def lookup[T](keys: Iterable[DocumentKey]): Cursor[VObject, List[T]] = Cursor[VObject, List[T]](
    query = "RETURN DOCUMENT(@@collection, @keys)",
    bindVars = VObject("@collection" -> name.toVPack, "keys" -> keys.toVPack)
  )
}
