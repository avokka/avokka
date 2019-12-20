package avokka.arangodb
package api

import avokka.velocypack._

case class DocumentCreate[T](
    document: T,
    returnNew: Boolean = false
) {
  def parameters = Map(
    "returnNew" -> returnNew.toString
  )
}

object DocumentCreate { self =>

  implicit def api[T: VPackDecoder](
      implicit e: VPackEncoder[T]): Api.Aux[Collection, DocumentCreate[T], T, Document.Response[T]] =
    new Api[Collection, DocumentCreate[T], T] {
      override type Response = Document.Response[T]
      override def header(collection: Collection, command: DocumentCreate[T]): Request.HeaderTrait = Request.Header(
        database = collection.database.name,
        requestType = RequestType.POST,
        request = s"/_api/document/${collection.name}",
        parameters = command.parameters
      )

      override def body(collection: Collection, command: DocumentCreate[T]): T = command.document
      override val encoder: VPackEncoder[T] = e.map {
        case VPack.VObject(values) => VPack.VObject(values -- Vector("_id", "_key", "_rev"))
        case v                     => v
      }
    }
}
