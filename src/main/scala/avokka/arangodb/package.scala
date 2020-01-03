package avokka

import avokka.velocypack._
import cats.data.EitherT
import cats.syntax.either._
import io.estatico.newtype.macros.newtype

import scala.concurrent.Future

import scala.language.{implicitConversions, higherKinds}

package object arangodb {

  type FEE[T] = EitherT[Future, ArangoError, T]

  @newtype case class DatabaseName(repr: String)
  object DatabaseName {
    implicit val encoder: VPackEncoder[DatabaseName] = deriving
    implicit val decoder: VPackDecoder[DatabaseName] = deriving
    val system: DatabaseName = DatabaseName("_system")
  }

  @newtype case class CollectionName(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }
  object CollectionName {
    implicit val encoder: VPackEncoder[CollectionName] = deriving
    implicit val decoder: VPackDecoder[CollectionName] = deriving
  }

  @newtype case class DocumentKey(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }
  object DocumentKey {
    val key: String = "_key"
    implicit val encoder: VPackEncoder[DocumentKey] = deriving
    implicit val decoder: VPackDecoder[DocumentKey] = deriving
    val empty = apply("")
  }

  @newtype case class DocumentHandle(repr: (CollectionName, DocumentKey)) {
    def collection: CollectionName = repr._1
    def key: DocumentKey = repr._2
    def isEmpty: Boolean = collection.isEmpty || key.isEmpty
    def path: String = if (isEmpty) "" else s"$collection/$key"
  }
  object DocumentHandle {
    val key: String = "_id"

    def apply(collection: CollectionName, key: DocumentKey): DocumentHandle =
      apply((collection, key))

    def parse(path: String): Option[DocumentHandle] = {
      path.split('/') match {
        case Array(collection, key) => Some(apply(CollectionName(collection), DocumentKey(key)))
        case _                      => None
      }
    }

    def apply(path: String): DocumentHandle = parse(path).getOrElse(empty)

    implicit val encoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
    implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].emap(path =>
      parse(path) match {
        case Some(value) => value.asRight
        case None        => VPackError.IllegalValue(s"invalid document handle '$path'").asLeft
    })

    val empty = apply(CollectionName(""), DocumentKey.empty)
  }

  @newtype case class DocumentRevision(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }
  object DocumentRevision {
    val key: String = "_rev"
    implicit val encoder: VPackEncoder[DocumentRevision] = deriving
    implicit val decoder: VPackDecoder[DocumentRevision] = deriving
    val empty = apply("")
  }

}
