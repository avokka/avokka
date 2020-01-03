package avokka.arangodb

import avokka.arangodb.api.{Cursor, DocumentRead}
import avokka.velocypack.VPack.VObject
import avokka.velocypack._

class Collection(val database: Database, val name: CollectionName) extends ApiContext[Collection] {

  override lazy val session: Session = database.session

  def handle(key: DocumentKey): DocumentHandle = DocumentHandle(name, key)

  def read[T: VPackDecoder](key: DocumentKey): DocumentRead[T] = DocumentRead[T](handle(key))

  def all[T]: Cursor[VObject, T] = Cursor[VObject, T](
    query = "FOR doc IN @@collection RETURN doc",
    bindVars = VObject("@collection" -> name.toVPack)
  )

  def lookup[T](keys: Iterable[DocumentKey]): Cursor[VObject, List[T]] = Cursor[VObject, List[T]](
    query = "RETURN DOCUMENT(@@collection, @keys)",
    bindVars = VObject("@collection" -> name.toVPack, "keys" -> keys.toVPack)
  )
}
