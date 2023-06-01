package avokka.arangodb.types

import avokka.velocypack._

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
      case _ => None
    }
  }

  def apply(path: String): DocumentHandle = parse(path).getOrElse(empty)

  implicit val encoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
  implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].flatMap { path =>
    parse(path).toRight(VPackError.IllegalValue(s"invalid document handle '$path'"))
  }

  val empty = apply(CollectionName(""), DocumentKey.empty)
}
