package avokka.arangodb
package api

import avokka.velocypack._
import types._

/**
  * Create document
  * @param document document value
  * @param waitForSync Wait until document has been synced to disk.   (optional)
  * @param returnNew Additionally return the complete new document under the attribute *new* in the result.   (optional)
  * @param returnOld Additionally return the complete old document under the attribute *old* in the result. Only available if the overwrite option is used.   (optional)
  * @param silent If set to *true*, an empty object will be returned as response. No meta-data  will be returned for the created document. This option can be used to save some network traffic.   (optional)
  * @param overwrite If set to *true*, the insert becomes a replace-insert. If a document with the same *_key* already exists the new document is not rejected with unique constraint violated but will replace the old document.   (optional)
  * @tparam T document type
  */
final case class DocumentCreate[T](
    collection: CollectionName,
    document: T,
    waitForSync: Boolean = false,
    returnNew: Boolean = false,
    returnOld: Boolean = false,
    silent: Boolean = false,
    overwrite: Boolean = false,
) {
  def parameters = Map(
    "waitForSync" -> waitForSync.toString,
    "returnNew" -> returnNew.toString,
    "returnOld" -> returnOld.toString,
    "silent" -> silent.toString,
    "overwrite" -> overwrite.toString,
  )
}

object DocumentCreate { self =>

/*  implicit def api[T: VPackDecoder: VPackEncoder]
    : Api.Aux[ArangoDatabase, DocumentCreate[T], T, Document.Response[T]] =
    new Api[ArangoDatabase, DocumentCreate[T], T] {
      override type Response = Document.Response[T]
      override def header(database: ArangoDatabase, command: DocumentCreate[T]): ArangoRequest.HeaderTrait =
        ArangoRequest.Header(
          database = database.name,
          requestType = RequestType.POST,
          request = s"/_api/document/${command.collection}",
          parameters = command.parameters
        )
      override def body(database: ArangoDatabase, command: DocumentCreate[T]): T = command.document
      override val encoder: VPackEncoder[T] =
        implicitly[VPackEncoder[T]].mapObject(_.filter(Document.filterEmptyInternalAttributes))
    }*/
}
