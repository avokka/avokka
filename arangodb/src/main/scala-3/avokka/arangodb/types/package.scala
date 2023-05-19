package avokka.arangodb

import avokka.velocypack._

package object types {

  case class DatabaseName(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object DatabaseName {
    implicit val encoder: VPackEncoder[DatabaseName] = VPackEncoder.stringEncoder.contramap(_.repr)
    implicit val decoder: VPackDecoder[DatabaseName] = VPackDecoder.stringDecoder.map(apply)
    val system: DatabaseName = DatabaseName("_system")
  }

  case class CollectionName(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object CollectionName {
    implicit val encoder: VPackEncoder[CollectionName] = VPackEncoder.stringEncoder.contramap(_.repr)
    implicit val decoder: VPackDecoder[CollectionName] = VPackDecoder.stringDecoder.map(apply)
  }

  case class DocumentKey(repr: String) {
    def isEmpty: Boolean = repr.isEmpty
  }

  object DocumentKey {
    val key: String = "_key"
    implicit val encoder: VPackEncoder[DocumentKey] = VPackEncoder.stringEncoder.contramap(_.repr)
    implicit val decoder: VPackDecoder[DocumentKey] = VPackDecoder.stringDecoder.map(apply)
    val empty = apply("")
  }

  case class DocumentHandle(collection: CollectionName, key: DocumentKey) {
    def isEmpty: Boolean = collection.isEmpty || key.isEmpty
    def path: String = if (isEmpty) "" else s"${collection.repr}/${key.repr}"

  }

  object DocumentHandle {
    val key: String = "_id"

    // def apply(handle: (CollectionName, DocumentKey)): DocumentHandle = handle

    def parse(path: String): Option[DocumentHandle] = {
      path.split('/') match {
        case Array(collection, key) => Some(apply(CollectionName(collection), DocumentKey(key)))
        case _                      => None
      }
    }

    def apply(path: String): DocumentHandle = parse(path).getOrElse(empty)

    implicit val encoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
    implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].flatMap { path =>
      parse(path).toRight(VPackError.IllegalValue(s"invalid document handle '$path'"))
    }

    val empty = apply(CollectionName(""), DocumentKey.empty)
  }

  case class DocumentRevision(repr: String)

  object DocumentRevision {
    val key: String = "_rev"
    implicit val encoder: VPackEncoder[DocumentRevision] = VPackEncoder[String].contramap(_.repr)
    implicit val decoder: VPackDecoder[DocumentRevision] = VPackDecoder[String].map(apply)
    val empty = apply("")
  }

  case class TransactionId(repr: String)

  object TransactionId {
    implicit val encoder: VPackEncoder[TransactionId] = VPackEncoder[String].contramap(_.repr)
    implicit val decoder: VPackDecoder[TransactionId] = VPackDecoder[String].map(apply)
  }

  case class GraphName(repr: String)

  object GraphName {
    implicit val encoder: VPackEncoder[GraphName] = VPackEncoder[String].contramap(_.repr)
    implicit val decoder: VPackDecoder[GraphName] = VPackDecoder[String].map(apply)
  }
}
