package avokka.arangodb.types

import avokka.velocypack._

case class DocumentHandle(collection: CollectionName, key: DocumentKey) {
  def isEmpty: Boolean = collection.isEmpty || key.isEmpty

  def path: String = if (isEmpty) "" else s"${collection.repr}/${key.repr}"

}

object DocumentHandle {
  val key: String = "_id"

  def parse(path: String): Either[VPackError, DocumentHandle] = {
    path.split('/') match {
      case Array(collection, key) => Right(apply(CollectionName(collection), DocumentKey(key)))
      case _ => Left(VPackError.IllegalValue(s"invalid document handle '$path'"))
    }
  }

  def apply(path: String): DocumentHandle = parse(path).getOrElse(empty)

  implicit val encoder: VPackEncoder[DocumentHandle] = VPackEncoder[String].contramap(_.path)
  implicit val decoder: VPackDecoder[DocumentHandle] = VPackDecoder[String].flatMap(parse)

  val empty = apply(CollectionName(""), DocumentKey.empty)
}
