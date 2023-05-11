package avokka.arangodb

import avokka.velocypack._

package object types {

  opaque type DatabaseName = String

  object DatabaseName {
    def apply(name: String): DatabaseName = name
    implicit val encoder: VPackEncoder[DatabaseName] = VPackEncoder.stringEncoder.contramap(_.toString)
    implicit val decoder: VPackDecoder[DatabaseName] = VPackDecoder.stringDecoder.map(apply)
    val system: DatabaseName = DatabaseName("_system")
  }

  opaque type CollectionName = String

  extension (name: CollectionName) {
    def repr: String = name
    def isEmpty: Boolean = name.isEmpty
  }

  object CollectionName {
    def apply(name: String): CollectionName = name
    implicit val encoder: VPackEncoder[CollectionName] = VPackEncoder.stringEncoder.contramap(_.toString)
    implicit val decoder: VPackDecoder[CollectionName] = VPackDecoder.stringDecoder.map(apply)
  }

  opaque type DocumentKey <: String = String

  /*
  extension (key: DocumentKey) {
    def isEmpty: Boolean = key.isEmpty
  }
  */

  object DocumentKey {
    def apply(key: String): DocumentKey = key
    val key: String = "_key"
    implicit val encoder: VPackEncoder[DocumentKey] = VPackEncoder.stringEncoder.contramap(_.toString)
    implicit val decoder: VPackDecoder[DocumentKey] = VPackDecoder.stringDecoder.map(apply)
    val empty = apply("")
  }

  opaque type DocumentHandle = (CollectionName, DocumentKey)

  extension (handle: DocumentHandle) {
    def collection: CollectionName = handle._1

    def key: DocumentKey = handle._2

    def isEmpty: Boolean = collection.isEmpty || key.isEmpty

    def path: String = if (isEmpty) "" else s"$collection/$key"
  }

  object DocumentHandle {
    val key: String = "_id"

    def apply(handle: (CollectionName, DocumentKey)): DocumentHandle = handle

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
    implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].flatMap { path =>
      parse(path).toRight(VPackError.IllegalValue(s"invalid document handle '$path'"))
    }

    val empty = apply(CollectionName(""), DocumentKey.empty)
  }

  opaque type DocumentRevision = String

  object DocumentRevision {
    def apply(str: String): DocumentRevision = str
    val key: String = "_rev"
    implicit val encoder: VPackEncoder[DocumentRevision] = VPackEncoder[String].contramap(_.toString)
    implicit val decoder: VPackDecoder[DocumentRevision] = VPackDecoder[String].map(apply)
    val empty = apply("")
  }

  opaque type TransactionId = String

  object TransactionId {
    def apply(s: String): TransactionId = s
    implicit val encoder: VPackEncoder[TransactionId] = VPackEncoder[String].contramap(_.toString)
    implicit val decoder: VPackDecoder[TransactionId] = VPackDecoder[String].map(apply)
  }

  opaque type GraphName = String

  object GraphName {
    def apply(s: String): GraphName = s
    implicit val encoder: VPackEncoder[GraphName] = VPackEncoder[String].contramap(_.toString)
    implicit val decoder: VPackDecoder[GraphName] = VPackDecoder[String].map(apply)
  }
}
