package avokka.arangodb

import avokka.velocypack._
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

// import cats.syntax.all._

package object types {

  private[types] type Result[T] = Either[Throwable, T]

  @newtype case class DatabaseName(repr: String)

  object DatabaseName {
    implicit val encoder: VPackEncoder[DatabaseName] = implicitly[VPackEncoder[String]].coerce
    implicit val decoder: VPackDecoder[DatabaseName] = implicitly[VPackDecoder[String]].coerce
    val system: DatabaseName = DatabaseName("_system")
  }

  @newtype case class CollectionName(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object CollectionName {
    implicit val encoder: VPackEncoder[CollectionName] = implicitly[VPackEncoder[String]].coerce
    implicit val decoder: VPackDecoder[CollectionName] = implicitly[VPackDecoder[String]].coerce
  }

  @newtype case class DocumentKey(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object DocumentKey {
    val key: String = "_key"
    implicit val encoder: VPackEncoder[DocumentKey] = implicitly[VPackEncoder[String]].coerce
    implicit val decoder: VPackDecoder[DocumentKey] = implicitly[VPackDecoder[String]].coerce
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
    implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].flatMapF { path =>
      parse(path).toRight(VPackError.IllegalValue(s"invalid document handle '$path'"))
    }

    val empty = apply(CollectionName(""), DocumentKey.empty)
  }

  @newtype case class DocumentRevision(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object DocumentRevision {
    val key: String = "_rev"
    implicit val encoder: VPackEncoder[DocumentRevision] = implicitly[VPackEncoder[String]].coerce
    implicit val decoder: VPackDecoder[DocumentRevision] = implicitly[VPackDecoder[String]].coerce
    val empty = apply("")
  }

}
