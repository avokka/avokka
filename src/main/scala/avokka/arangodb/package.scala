package avokka

import avokka.velocypack._
import cats.data.EitherT
import shapeless.tag
import shapeless.tag.@@
import cats.syntax.contravariant._

import scala.concurrent.Future

package object arangodb {

  type FEE[T] = EitherT[Future, VPackError, T]

  trait DatabaseNameTag
  type DatabaseName = String @@ DatabaseNameTag
  def DatabaseName(value: String): DatabaseName = tag[DatabaseNameTag][String](value)

  implicit val databaseNameEncoder: VPackEncoder[DatabaseName] = VPackEncoder[String].narrow
  implicit val databaseNameDecoder: VPackDecoder[DatabaseName] = VPackDecoder[String].map(DatabaseName)

  trait CollectionNameTag
  type CollectionName = String @@ CollectionNameTag
  def CollectionName(value: String): CollectionName = tag[CollectionNameTag][String](value)

  implicit val collectionNameEncoder: VPackEncoder[CollectionName] = VPackEncoder[String].narrow
  implicit val collectionNameDecoder: VPackDecoder[CollectionName] = VPackDecoder[String].map(CollectionName)

  trait DocumentKeyTag
  type DocumentKey = String @@ DocumentKeyTag
  def DocumentKey(value: String): DocumentKey = tag[DocumentKeyTag][String](value)

  implicit val documentKeyEncoder: VPackEncoder[DocumentKey] = VPackEncoder[String].narrow
  implicit val documentKeyDecoder: VPackDecoder[DocumentKey] = VPackDecoder[String].map(DocumentKey)

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

  implicit val documentHandleEncoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
  implicit val documentHandleDecoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].map(DocumentHandle.apply)

}
