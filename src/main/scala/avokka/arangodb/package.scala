package avokka

import scodec.Codec
import shapeless.tag
import shapeless.tag.@@

package object arangodb {

  trait DatabaseNameTag
  type DatabaseName = String @@ DatabaseNameTag
  def DatabaseName(value: String): DatabaseName = tag[DatabaseNameTag][String](value)

  implicit val databaseNameCodec: Codec[DatabaseName] = velocypack.stringCodec.xmap(DatabaseName, _.asInstanceOf[String])

  trait CollectionNameTag
  type CollectionName = String @@ CollectionNameTag
  def CollectionName(value: String): CollectionName = tag[CollectionNameTag][String](value)

  trait DocumentKeyTag
  type DocumentKey = String @@ DocumentKeyTag
  def DocumentKey(value: String): DocumentKey = tag[DocumentKeyTag][String](value)

  implicit val documentKeyCodec: Codec[DocumentKey] = velocypack.stringCodec.xmap(DocumentKey, _.asInstanceOf[String])

  case class DocumentHandle
  (
    collection: CollectionName,
    key: DocumentKey
  ) {
    def path: String = s"$collection/$key"
  }

  object DocumentHandle {
    def apply(path: String): DocumentHandle = {
      val parts = path.split('/')
      DocumentHandle(CollectionName(parts(0)), DocumentKey(parts(1)))
    }
  }

  implicit val documentHandleCodec: Codec[DocumentHandle] = velocypack.stringCodec.xmap(DocumentHandle.apply, _.path)

}
