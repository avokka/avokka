package avokka.arangodb.api

import avokka.arangodb._
import avokka.velocypack.VPack.VObject
import avokka.velocypack._

case class DocumentCreate[T]
(
  document: T,
  returnNew: Boolean = false
) {
  def parameters = Map(
    "returnNew" -> returnNew.toString
  )
}

object DocumentCreate { self =>

  case class Response[T]
  (
    _id: DocumentHandle,
    _key: DocumentKey,
    _rev: String,
    `new`: Option[T] = None,
    old: Option[T] = None,
  )

  object Response {
    implicit def decoder[T : VPackDecoder]: VPackDecoder[Response[T]] = VPackRecord[Response[T]].decoderWithDefaults
  }

  implicit def api[T : VPackDecoder](implicit encoder: VPackEncoder[T]): Api.Aux[Collection, DocumentCreate[T], T, Response[T]] = new Api[Collection, DocumentCreate[T], T] {
    override type Response = self.Response[T]
    override def requestHeader(collection: Collection, command: DocumentCreate[T]): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.POST,
      request = s"/_api/document/${collection.name}",
      parameters = command.parameters
    )

    override def body(collection: Collection, command: DocumentCreate[T]): T = command.document
    override val bodyEncoder: VPackEncoder[T] = encoder.map {
      case VObject(values) => VObject(values -- Vector("_id", "_key", "_rev"))
      case v => v
    }
  }
}
