package avokka.arangodb.api

import avokka.arangodb.{api, _}
import avokka.velocypack.VPack.VObject
import avokka.velocypack._

case class DocumentCreate[T]
(
  document: T,
  returnNew: Boolean = false
)

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
    implicit def decoder[T](implicit d: VPackDecoder[T]): VPackDecoder[Response[T]] = VPackRecord[Response[T]].decoderWithDefaults
  }

  implicit def api[T](implicit encoder: VPackEncoder[T], decoder: VPackDecoder[T]): ApiPayload.Aux[Collection, DocumentCreate[T], T, Response[T]] = new ApiPayload[Collection, DocumentCreate[T], T] {
    override def requestHeader(collection: Collection, command: DocumentCreate[T]): Request.HeaderTrait = Request.Header(
      database = collection.database.name,
      requestType = RequestType.POST,
      request = s"/_api/document/${collection.name}",
      parameters = Map(
        "returnNew" -> command.returnNew.toString
      )
    )

    override def body(collection: Collection, command: DocumentCreate[T]): T = command.document
    override val bodyEncoder: VPackEncoder[T] = encoder.map {
      case VObject(values) => VObject(values -- Seq("_key", "_id"))
      case v => v
    }
    override type Response = self.Response[T]
//    override val responseDecoder: VPackDecoder[Response] = Response.decoder
  }
}
