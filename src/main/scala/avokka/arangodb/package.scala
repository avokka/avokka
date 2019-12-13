package avokka

import avokka.velocypack._
import scodec.Codec
import scodec.codecs.provide
import shapeless.tag
import shapeless.tag.@@

package object arangodb {

  trait DatabaseNameTag
  type DatabaseName = String @@ DatabaseNameTag
  def DatabaseName(value: String): DatabaseName = tag[DatabaseNameTag][String](value)

  implicit val databaseNameEncoder: VPackEncoder[DatabaseName] = VPackEncoder.stringEncoder.contramap(_.asInstanceOf[String])
  implicit val databaseNameDecoder: VPackDecoder[DatabaseName] = VPackDecoder.stringDecoder.map(DatabaseName)

  trait CollectionNameTag
  type CollectionName = String @@ CollectionNameTag
  def CollectionName(value: String): CollectionName = tag[CollectionNameTag][String](value)

  implicit val collectionNameEncoder: VPackEncoder[CollectionName] = VPackEncoder.stringEncoder.contramap(_.asInstanceOf[String])
  implicit val collectionNameDecoder: VPackDecoder[CollectionName] = VPackDecoder.stringDecoder.map(CollectionName)

  trait DocumentKeyTag
  type DocumentKey = String @@ DocumentKeyTag
  def DocumentKey(value: String): DocumentKey = tag[DocumentKeyTag][String](value)

  implicit val documentKeyEncoder: VPackEncoder[DocumentKey] = VPackEncoder.stringEncoder.contramap(_.asInstanceOf[String])
  implicit val documentKeyDecoder: VPackDecoder[DocumentKey] = VPackDecoder.stringDecoder.map(DocumentKey)

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

    val empty = DocumentHandle(CollectionName(""), DocumentKey(""))
  }

  implicit val documentHandleEncoder: VPackEncoder[DocumentHandle] = VPackEncoder.stringEncoder.contramap(_.path)
  implicit val documentHandleDecoder: VPackDecoder[DocumentHandle] = VPackDecoder.stringDecoder.map(DocumentHandle.apply)

  implicit val unitCodec: Codec[Unit] = provide(())

}
