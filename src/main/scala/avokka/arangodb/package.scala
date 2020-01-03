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

  @newtype case class CollectionName(repr: String)
  object CollectionName {
    implicit val encoder: VPackEncoder[CollectionName] = deriving
    implicit val decoder: VPackDecoder[CollectionName] = deriving
  }

  @newtype case class DocumentKey(repr: String)
  object DocumentKey {
    implicit val encoder: VPackEncoder[DocumentKey] = deriving
    implicit val decoder: VPackDecoder[DocumentKey] = deriving
    val Empty = apply("")
  }

  @newtype case class DocumentHandle(repr: (CollectionName, DocumentKey)) {
    def collection: CollectionName = repr._1
    def key: DocumentKey = repr._2
    def path: String = s"$collection/$key"
  }
  object DocumentHandle {

    def apply(collection: CollectionName, key: DocumentKey): DocumentHandle =
      apply((collection, key))

    def parse(path: String): Option[DocumentHandle] = {
      path.split('/') match {
        case Array(collection, key) => Some(apply(CollectionName(collection), DocumentKey(key)))
        case _                      => None
      }
    }

    implicit val encoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
    implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].emap(path =>
      parse(path) match {
        case Some(value) => value.asRight
        case None        => VPackError.IllegalValue(s"invalid document handle '$path'").asLeft
    })

    val Empty = apply(CollectionName(""), DocumentKey.Empty)
  }

}
